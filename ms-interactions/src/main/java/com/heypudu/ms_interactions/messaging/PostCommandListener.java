package com.heypudu.ms_interactions.messaging;

import com.heypudu.ms_interactions.config.amqp.RabbitTopology;
import com.heypudu.ms_interactions.service.CommentService;
import com.heypudu.ms_interactions.service.LikeService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de los comandos que afectan a las interacciones.
 *
 * <p>Usa ACK manual: el mensaje solo se confirma cuando el borrado termina
 * bien. Si falla, se rechaza sin reencolar y RabbitMQ lo envia a la DLQ, donde
 * {@link DeadLetterListener} lo registra.</p>
 */
@Component
public class PostCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PostCommandListener.class);

    private final LikeService likeService;
    private final CommentService commentService;

    public PostCommandListener(LikeService likeService, CommentService commentService) {
        this.likeService = likeService;
        this.commentService = commentService;
    }

    @RabbitListener(queues = RabbitTopology.COMMAND_QUEUE)
    public void onPostDeleted(PostDeletedMessage message,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            commentService.deleteAllByPost(message.postId());
            likeService.deleteAllByPost(message.postId());
            channel.basicAck(deliveryTag, false);
            log.info("Interacciones de la publicacion {} eliminadas", message.postId());
        } catch (Exception ex) {
            log.error("No se pudieron eliminar las interacciones de {}: {}",
                    message.postId(), ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
