package com.heypudu.ms_interactions.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID userId,
        String content,
        Instant createdAt
) {
}