package com.heypudu.ms_auth.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        UUID cognitoSub,
        String name,
        String username,
        String avatarUrl,
        boolean isVerified,
        String status
) {
}