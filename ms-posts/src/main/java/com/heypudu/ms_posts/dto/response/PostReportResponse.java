package com.heypudu.ms_posts.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PostReportResponse(
        UUID id,
        UUID postId,
        String postTitle,
        String postAuthorUsername,
        String audioUrl,
        Integer audioDurationSeconds,
        UUID reporterId,
        String reason,
        String status,
        Instant createdAt,
        Instant resolvedAt
) {
}
