package com.heypudu.ms_live.service.impl;

import com.heypudu.ms_live.client.UserClient;
import com.heypudu.ms_live.dto.request.CreateLiveRoomRequest;
import com.heypudu.ms_live.dto.response.LiveRoomAccessResponse;
import com.heypudu.ms_live.dto.response.LiveRoomResponse;
import com.heypudu.ms_live.exception.BusinessException;
import com.heypudu.ms_live.exception.ResourceNotFoundException;
import com.heypudu.ms_live.model.LiveRoom;
import com.heypudu.ms_live.model.LiveRoomStatus;
import com.heypudu.ms_live.repository.LiveRoomRepository;
import com.heypudu.ms_live.security.CognitoUserExtractor;
import com.heypudu.ms_live.service.LiveRoomService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LiveRoomServiceImpl implements LiveRoomService {

    private final LiveRoomRepository repository;
    private final CognitoUserExtractor cognitoUserExtractor;
    private final UserClient userClient;
    private final String mediamtxPublicUrl;

    public LiveRoomServiceImpl(
            LiveRoomRepository repository,
            CognitoUserExtractor cognitoUserExtractor,
            UserClient userClient,
            @Value("${mediamtx.public-url:http://localhost:8889}") String mediamtxPublicUrl) {
        this.repository = repository;
        this.cognitoUserExtractor = cognitoUserExtractor;
        this.userClient = userClient;
        this.mediamtxPublicUrl = mediamtxPublicUrl.replaceAll("/+$", "");
    }

    @Override
    @Transactional
    public LiveRoomResponse createRoom(CreateLiveRoomRequest request, Jwt jwt) {
        UUID creatorSub = cognitoUserExtractor.extractSub(jwt);

        repository.findByCreatorCognitoSubAndStatus(creatorSub, LiveRoomStatus.LIVE)
                .ifPresent(room -> {
                    throw new BusinessException("El usuario ya tiene una sala activa");
                });

        LiveRoom room = new LiveRoom(creatorSub, creatorSub, request.title(), request.description());
        LiveRoom saved = repository.save(room);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void closeRoom(UUID roomId, Jwt jwt) {
        LiveRoom room = findRoom(roomId);
        UUID requesterSub = cognitoUserExtractor.extractSub(jwt);

        if (!room.getCreatorCognitoSub().equals(requesterSub)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo el creador puede cerrar la sala");
        }

        room.close();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveRoomResponse> listActiveRooms() {
        return repository.findByStatusOrderByStartedAtDesc(LiveRoomStatus.LIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LiveRoomResponse getRoom(UUID roomId) {
        return toResponse(findRoom(roomId));
    }

    @Override
    @Transactional(readOnly = true)
    public LiveRoomAccessResponse checkAccess(UUID roomId, Jwt jwt) {
        LiveRoom room = findRoom(roomId);
        UUID listenerSub = cognitoUserExtractor.extractSub(jwt);

        if (room.getCreatorCognitoSub().equals(listenerSub)) {
            return new LiveRoomAccessResponse(true);
        }
        if (room.getStatus() != LiveRoomStatus.LIVE) {
            return new LiveRoomAccessResponse(false);
        }

        boolean following = userClient.isFollowing(
                room.getCreatorCognitoSub(),
                listenerSub,
                "Bearer " + jwt.getTokenValue());
        return new LiveRoomAccessResponse(following);
    }

    private LiveRoom findRoom(UUID roomId) {
        return repository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
    }

    private LiveRoomResponse toResponse(LiveRoom room) {
        return new LiveRoomResponse(
                room.getId(),
                room.getTitle(),
                room.getDescription(),
                room.getStatus(),
                room.getCreatorCognitoSub(),
                mediamtxPublicUrl + "/" + room.getId() + "/whip",
                mediamtxPublicUrl + "/" + room.getId() + "/whep",
                room.getStartedAt(),
                room.getEndedAt());
    }
}
