package com.heypudu.ms_auth.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        UUID cognitoSub,
        String email,
        String status,
        List<String> roles,
        Instant createdAt
) {
}