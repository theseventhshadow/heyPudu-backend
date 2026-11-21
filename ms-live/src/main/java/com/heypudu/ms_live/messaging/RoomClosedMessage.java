package com.heypudu.ms_live.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Comando publicado en {@code ex.heypudu.cmd} con routing key
 * {@code room.closed} y consumido por {@code ms-live}.
 */
public record RoomClosedMessage(
        UUID eventId,
        UUID roomId,
        Instant occurredAt
) {
}
