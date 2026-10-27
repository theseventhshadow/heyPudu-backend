package com.heypudu.ms_interactions.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentReportResponse(
        UUID id,
        UUID commentId,
        UUID postId,
        String commentContent,
        UUID reporterId,
        String reason,
        String status,
        Instant createdAt,
        Instant resolvedAt
) {
}
