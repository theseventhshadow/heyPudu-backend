package com.heypudu.ms_live.service;

import com.heypudu.ms_live.client.UserClient;
import com.heypudu.ms_live.dto.mediamtx.MediamtxAuthRequest;
import com.heypudu.ms_live.dto.mediamtx.MediamtxHookRequest;
import com.heypudu.ms_live.exception.BusinessException;
import com.heypudu.ms_live.messaging.LiveEventPublisher;
import com.heypudu.ms_live.model.LiveRoom;
import com.heypudu.ms_live.model.LiveRoomStatus;
import com.heypudu.ms_live.repository.LiveRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Decisiones de autorización para MediaMTX.
 *
 * <ul>
 *   <li>{@code publish}: solo el creador de la sala.</li>
 *   <li>{@code read}: el creador o un seguidor del creador.</li>
 *   <li>Identidad D3: {@code user = cognitoSub}, {@code password/token = JWT};
 *       el {@code sub} del JWT debe coincidir con {@code user}.</li>
 * </ul>
 */
@Service
public class MediamtxAuthService {

    private static final Logger log = LoggerFactory.getLogger(MediamtxAuthService.class);

    private static final String ACTION_PUBLISH = "publish";
    private static final String ACTION_READ = "read";
    private static final String HOOK_JOINED = "JOINED";
    private static final String HOOK_LEFT = "LEFT";

    private final LiveRoomRepository roomRepository;
    private final UserClient userClient;
    private final JwtDecoder jwtDecoder;
    private final LiveEventPublisher eventPublisher;

    public MediamtxAuthService(LiveRoomRepository roomRepository,
                               UserClient userClient,
                               JwtDecoder jwtDecoder,
                               LiveEventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.userClient = userClient;
        this.jwtDecoder = jwtDecoder;
        this.eventPublisher = eventPublisher;
    }

    public void authorize(MediamtxAuthRequest request) {
        if (request == null || request.action() == null || request.path() == null) {
            throw new BadCredentialsException("Solicitud de autorizacion incompleta");
        }
        if (!ACTION_PUBLISH.equals(request.action()) && !ACTION_READ.equals(request.action())) {
            throw new AccessDeniedException("Accion no permitida: " + request.action());
        }

        LiveRoom room = findLiveRoom(request.path());
        AuthenticatedIdentity identity = authenticate(request);

        if (ACTION_PUBLISH.equals(request.action())) {
            if (!room.getCreatorCognitoSub().equals(identity.cognitoSub())) {
                throw new AccessDeniedException("Solo el creador puede publicar en esta sala");
            }
            return;
        }

        if (room.getCreatorCognitoSub().equals(identity.cognitoSub())) {
            return;
        }

        boolean following;
        try {
            following = userClient.isFollowing(
                    room.getCreatorCognitoSub(),
                    identity.cognitoSub(),
                    "Bearer " + identity.token());
        } catch (RuntimeException ex) {
            log.error("No se pudo verificar el seguimiento | roomId={} | listener={}",
                    room.getId(), identity.cognitoSub(), ex);
            throw new AccessDeniedException("No se pudo verificar el acceso a la sala");
        }
        if (!following) {
            throw new AccessDeniedException("Debes seguir al creador para escuchar la sala");
        }
    }

    public void handleHook(MediamtxHookRequest request) {
        if (request == null || request.path() == null || request.action() == null) {
            return;
        }
        String action = request.action().trim().toUpperCase();
        if (!HOOK_JOINED.equals(action) && !HOOK_LEFT.equals(action)) {
            throw new BusinessException("Accion de hook no soportada: " + request.action());
        }

        UUID listenerSub = extractUser(request.query());
        if (listenerSub == null) {
            // Sin identidad no se puede atribuir el oyente; se ignora el hook.
            log.warn("Hook de MediaMTX sin usuario | path={} | action={}", request.path(), action);
            return;
        }

        LiveRoom room;
        try {
            room = findLiveRoom(request.path());
        } catch (RuntimeException ex) {
            log.warn("Hook de MediaMTX para sala inexistente o inactiva | path={}", request.path());
            return;
        }

        if (HOOK_JOINED.equals(action)) {
            eventPublisher.listenerJoined(room, listenerSub);
        } else {
            eventPublisher.listenerLeft(room, listenerSub);
        }
    }

    private LiveRoom findLiveRoom(String path) {
        UUID roomId;
        try {
            roomId = UUID.fromString(path);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Path de sala invalido");
        }
        LiveRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AccessDeniedException("Sala no encontrada"));
        if (room.getStatus() != LiveRoomStatus.LIVE) {
            throw new AccessDeniedException("La sala no esta activa");
        }
        return room;
    }

    private AuthenticatedIdentity authenticate(MediamtxAuthRequest request) {
        String token = firstNonBlank(request.token(), looksLikeJwt(request.password()) ? request.password() : null);
        if (token == null) {
            throw new BadCredentialsException("Credenciales incompletas");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException ex) {
            throw new BadCredentialsException("Token invalido");
        }

        UUID tokenSub;
        try {
            tokenSub = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new BadCredentialsException("El token no contiene un sub valido");
        }

        // Si MediaMTX envía user (Basic user/password), debe coincidir con el sub.
        // Con Authorization: Bearer el usuario se deduce del propio token.
        if (request.user() != null && !request.user().isBlank()) {
            UUID claimedSub;
            try {
                claimedSub = UUID.fromString(request.user());
            } catch (IllegalArgumentException ex) {
                throw new BadCredentialsException("Usuario invalido");
            }
            if (!tokenSub.equals(claimedSub)) {
                throw new BadCredentialsException("El usuario no coincide con el token");
            }
        }
        return new AuthenticatedIdentity(tokenSub, token);
    }

    /**
     * Extrae el {@code user} del query que MediaMTX pasa a los hooks.
     *
     * <p>MediaMTX publica {@code MTX_QUERY} con {@code url.QueryEscape}, así que
     * {@code user=<sub>} llega como {@code user%3D<sub>}. Se decodifica antes de
     * partir por {@code &} y {@code =}.
     */
    static UUID extractUser(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String decoded = query;
        try {
            decoded = URLDecoder.decode(query, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            // Si no se puede decodificar se intenta con el valor recibido.
        }
        String normalized = decoded.startsWith("?") ? decoded.substring(1) : decoded;
        for (String pair : normalized.split("&")) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            if ("user".equals(pair.substring(0, separator))) {
                try {
                    return UUID.fromString(pair.substring(separator + 1));
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean looksLikeJwt(String value) {
        return value != null && value.chars().filter(ch -> ch == '.').count() == 2;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private record AuthenticatedIdentity(UUID cognitoSub, String token) {
    }
}
