package com.heypudu.ms_auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SyncUserResponse(
        UUID id,
        UUID cognitoSub,
        String email,
        String status,
        boolean created,
        Instant createdAt
) {
}