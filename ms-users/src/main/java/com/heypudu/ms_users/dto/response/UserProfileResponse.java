package com.heypudu.ms_users.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID cognitoSub,
        String name,
        String username,
        String bio,
        String avatarUrl,
        boolean isVerified,
        String status,
        int followerCount,
        int followingCount,
        int postCount,
        Instant usernameUpdatedAt,
        Instant anonymizedAt,
        Instant createdAt,
        Instant updatedAt
) {
}