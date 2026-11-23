package com.heypudu.ms_posts.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de publicación que viaja por el tópico {@code heypudu.posts.events}.
 *
 * <p>La clave del mensaje es el {@code postId}, de modo que todos los eventos de
 * una publicación caen en la misma partición y se consumen en orden.</p>
 *
 * @param eventId        identificador del evento, para deduplicar en el consumidor
 * @param eventType      creación o eliminación
 * @param postId         publicación afectada
 * @param authorCognitoSub autor de la publicación
 * @param postType       tipo de publicación (AUDIO o TEXT)
 * @param occurredAt     instante del evento en UTC
 * @param schemaVersion  versión del contrato del mensaje
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
