package com.heypudu.ms_users.messaging;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID actorId,
        UUID recipientId,
        UUID postId,
        UUID commentId,
        UUID roomId,
        String roomTitle,
        UUID actorCognitoSub
) {

    /**
     * Constructor compatible con los productores existentes (like, comentario,
     * seguimiento), que no envían datos de sala.
     */
    public NotificationEvent(UUID eventId,
                             Instant occurredAt,
                             UUID actorId,
                             UUID recipientId,
                             UUID postId,
                             UUID commentId) {
        this(eventId, occurredAt, actorId, recipientId, postId, commentId, null, null, null);
    }
}
