package com.heypudu.ms_live.dto.mediamtx;

/**
 * Cuerpo que MediaMTX envía a {@code POST /api/v1/internal/mediamtx/auth}
 * cuando {@code authMethod: http}.
 */
public record MediamtxAuthRequest(
        String user,
        String password,
        String token,
        String ip,
        String action,
        String path,
        String protocol,
        String id,
        String query
) {
}
