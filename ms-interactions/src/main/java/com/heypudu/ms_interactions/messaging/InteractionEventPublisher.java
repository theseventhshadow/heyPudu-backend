package com.heypudu.ms_interactions.messaging;

import com.heypudu.ms_interactions.config.amqp.RabbitTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class InteractionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InteractionEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public InteractionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishLike(UUID actorId, UUID recipientId, UUID postId) {
        publish(
                RabbitTopology.RK_INTERACTION_LIKE,
                new NotificationEvent(UUID.randomUUID(), Instant.now(), actorId, recipientId, postId, null)
        );
    }

    public void publishComment(UUID actorId, UUID recipientId, UUID postId, UUID commentId) {
        publish(
                RabbitTopology.RK_INTERACTION_COMMENT,
                new NotificationEvent(UUID.randomUUID(), Instant.now(), actorId, recipientId, postId, commentId)
        );
    }

    private void publish(String routingKey, NotificationEvent event) {
        rabbitTemplate.convertAndSend(RabbitTopology.EVENTS_EXCHANGE, routingKey, event);
        log.info("Evento {} publicado | eventId={} | actorId={} | recipientId={}",
                routingKey, event.eventId(), event.actorId(), event.recipientId());
    }
}
