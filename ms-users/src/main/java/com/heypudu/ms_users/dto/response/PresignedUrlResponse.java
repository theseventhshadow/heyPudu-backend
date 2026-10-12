package com.heypudu.ms_users.dto.response;

import java.time.Instant;

public record PresignedUrlResponse(
        String uploadUrl,
        String objectKey,
        Instant expiresAt
) {
}