package com.heypudu.ms_live.dto.mediamtx;

/**
 * Cuerpo que envían los hooks {@code runOnRead}/{@code runOnUnread} de
 * MediaMTX. {@code query} transporta los parámetros de la URL del oyente
 * (se usa {@code user=<cognitoSub>} para identificar quién escucha).
 */
public record MediamtxHookRequest(
        String path,
        String action,
        String query
) {
}
