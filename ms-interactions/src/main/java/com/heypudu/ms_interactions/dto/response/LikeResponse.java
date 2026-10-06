package com.heypudu.ms_interactions.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LikeResponse(
        UUID id,
        UUID postId,
        UUID userId,
        Instant createdAt
) {
}