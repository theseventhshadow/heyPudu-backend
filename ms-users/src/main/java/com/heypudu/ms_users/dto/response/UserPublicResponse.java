package com.heypudu.ms_users.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserPublicResponse(
        UUID id,
        String name,
        String username,
        String bio,
        String avatarUrl,
        boolean isVerified,
        boolean isBanned,
        int followerCount,
        int followingCount,
        int postCount,
        Instant createdAt
) {
}