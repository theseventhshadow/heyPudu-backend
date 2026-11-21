package com.heypudu.ms_live.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato de los eventos publicados en el tópico {@code heypudu.live.events}.
 * La clave del mensaje es siempre {@code roomId} para conservar el orden por sala.
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
