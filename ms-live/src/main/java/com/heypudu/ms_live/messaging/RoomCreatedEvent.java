package com.heypudu.ms_live.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado en {@code ex.heypudu.events} con routing key
 * {@code room.created}. Los nombres de campo coinciden con el
 * {@code NotificationEvent} de {@code ms-users}.
 */
public record RoomCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID roomId,
        UUID actorCognitoSub,
        String roomTitle
) {
}
