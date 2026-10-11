package com.heypudu.ms_posts.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PostSummaryResponse(
        UUID id,
        UUID authorId,
        String authorUsername,
        String authorAvatarUrl,
        String type,
        String title,
        String coverImageUrl,
        int likeCount,
        int commentCount,
        Instant createdAt
) {
}