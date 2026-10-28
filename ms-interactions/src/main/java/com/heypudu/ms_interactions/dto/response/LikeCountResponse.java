package com.heypudu.ms_interactions.dto.response;

import java.util.UUID;

public record LikeCountResponse(
        UUID postId,
        long count
) {
}