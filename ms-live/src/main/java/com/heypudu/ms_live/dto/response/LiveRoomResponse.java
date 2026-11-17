package com.heypudu.ms_live.dto.response;

import com.heypudu.ms_live.model.LiveRoomStatus;

import java.time.Instant;
import java.util.UUID;

public record LiveRoomResponse(
        UUID id,
        String title,
        String description,
        LiveRoomStatus status,
        UUID creatorCognitoSub,
        String whipUrl,
        String whepUrl,
        Instant startedAt,
        Instant endedAt
) {
}
