package com.heypudu.ms_analytics.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia del evento publicado por {@code ms-posts} en {@code heypudu.posts.events}.
 *
 * <p>El productor no envía cabeceras de tipo, así que el consumidor fija este
 * DTO con {@code spring.json.value.default.type}.</p>
 */
public record PostEvent(
        UUID eventId,
        PostEventType eventType,
        UUID postId,
        UUID authorCognitoSub,
        String postType,
        Instant occurredAt,
        int schemaVersion) {
}
