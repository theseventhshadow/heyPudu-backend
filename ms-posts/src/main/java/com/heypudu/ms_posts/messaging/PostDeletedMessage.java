package com.heypudu.ms_posts.messaging;

import java.util.UUID;

/**
 * Comando que se publica cuando una publicacion se elimina, para que
 * ms-interactions borre sus likes y comentarios de forma asincrona.
 */
public record PostDeletedMessage(UUID postId) {
}
