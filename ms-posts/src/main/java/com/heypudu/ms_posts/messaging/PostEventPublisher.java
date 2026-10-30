package com.heypudu.ms_posts.messaging;

import com.heypudu.ms_posts.config.amqp.RabbitTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publica los comandos y eventos de ms-posts.
 *
 * <p>El envio es asincrono: el usuario recibe la respuesta sin esperar a que
 * ms-interactions procese el borrado. Si el broker no esta disponible, el
 * envio lanza excepcion y la transaccion de borrado se revierte.</p>
 */
@Component
public class PostEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PostEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PostEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPostDeleted(UUID postId) {
        rabbitTemplate.convertAndSend(
                RabbitTopology.COMMAND_EXCHANGE,
                RabbitTopology.RK_POST_DELETE,
                new PostDeletedMessage(postId));
        log.info("Comando {} publicado para la publicacion {}", RabbitTopology.RK_POST_DELETE, postId);
    }
}
