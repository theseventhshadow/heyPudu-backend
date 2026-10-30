package com.heypudu.ms_interactions.messaging;

import java.util.UUID;

/**
 * Espejo del comando que publica ms-posts al eliminar una publicacion.
 */
public record PostDeletedMessage(UUID postId) {
}
