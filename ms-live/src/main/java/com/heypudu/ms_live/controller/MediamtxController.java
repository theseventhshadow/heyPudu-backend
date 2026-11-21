package com.heypudu.ms_live.controller;

import com.heypudu.ms_live.dto.mediamtx.MediamtxAuthRequest;
import com.heypudu.ms_live.dto.mediamtx.MediamtxHookRequest;
import com.heypudu.ms_live.service.MediamtxAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Endpoints internos consumidos por MediaMTX. Se autentican con
 * {@code MEDIAMTX_SERVICE_TOKEN} y solo deben ser accesibles por la VPC.
 */
@RestController
@RequestMapping("/api/v1/internal/mediamtx")
public class MediamtxController {

    private static final String SERVICE_TOKEN_HEADER = "X-Mediamtx-Token";

    private final MediamtxAuthService authService;
    private final String serviceToken;

    public MediamtxController(MediamtxAuthService authService,
                              @Value("${mediamtx.service-token:}") String serviceToken) {
        this.authService = authService;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/auth")
    public ResponseEntity<Void> authorize(
            @RequestHeader(name = SERVICE_TOKEN_HEADER, required = false) String headerToken,
            @RequestParam(name = "serviceToken", required = false) String queryToken,
            @RequestBody MediamtxAuthRequest request) {
        validateServiceToken(headerToken, queryToken);
        authService.authorize(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hooks")
    public ResponseEntity<Void> hooks(
            @RequestHeader(name = SERVICE_TOKEN_HEADER, required = false) String headerToken,
            @RequestParam(name = "serviceToken", required = false) String queryToken,
            @RequestBody MediamtxHookRequest request) {
        validateServiceToken(headerToken, queryToken);
        authService.handleHook(request);
        return ResponseEntity.accepted().build();
    }

    private void validateServiceToken(String headerToken, String queryToken) {
        String provided = headerToken != null && !headerToken.isBlank() ? headerToken : queryToken;
        byte[] expected = serviceToken == null ? new byte[0] : serviceToken.getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, actual)) {
            throw new BadCredentialsException("Token de servicio invalido");
        }
    }
}
