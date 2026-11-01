package com.heypudu.ms_posts.messaging;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID eventId,
        Instant occurredAt,
        UUID actorId,
        UUID recipientId,
        UUID postId,
        UUID commentId
) {
}
