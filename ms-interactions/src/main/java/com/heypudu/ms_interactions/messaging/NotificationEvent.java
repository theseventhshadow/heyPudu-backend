package com.heypudu.ms_interactions.messaging;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID actorId,
        UUID recipientId,
        UUID postId,
        UUID commentId
) {
}
