package com.heypudu.ms_analytics.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia local del contrato publicado por {@code ms-live}.
 *
 * <p>Se fija como tipo destino del deserializador para no compartir paquetes
 * entre servicios ni depender de las cabeceras de tipo.
 */
public record LiveRoomEvent(
        UUID eventId,
        LiveEventType eventType,
        UUID roomId,
        UUID creatorCognitoSub,
        UUID actorCognitoSub,
        ReactionType reactionType,
        Instant occurredAt,
        int schemaVersion
) {
}
