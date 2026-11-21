package com.heypudu.ms_live.client;

import java.util.UUID;

/**
 * Resumen del creador que expone el endpoint interno de {@code ms-users}.
 */
public record UserSummaryDto(
        UUID id,
        UUID cognitoSub,
        String name,
        String username,
        String avatarUrl,
        boolean isVerified,
        String status
) {
}
