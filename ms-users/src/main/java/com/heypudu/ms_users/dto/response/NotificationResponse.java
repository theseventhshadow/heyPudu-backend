package com.heypudu.ms_users.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID actorId,
        String type,
        UUID postId,
        UUID commentId,
        String message,
        boolean read,
        Instant createdAt
) {
}
