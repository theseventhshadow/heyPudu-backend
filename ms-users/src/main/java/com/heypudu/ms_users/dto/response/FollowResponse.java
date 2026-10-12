package com.heypudu.ms_users.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FollowResponse(
        UUID id,
        UUID followerId,
        UUID followedId,
        Instant createdAt
) {
}