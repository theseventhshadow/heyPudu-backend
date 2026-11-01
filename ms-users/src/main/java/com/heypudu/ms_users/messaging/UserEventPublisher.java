package com.heypudu.ms_users.messaging;

import com.heypudu.ms_users.config.amqp.RabbitTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishFollow(UUID actorId, UUID recipientId) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                Instant.now(),
                actorId,
                recipientId,
                null,
                null
        );
        rabbitTemplate.convertAndSend(
                RabbitTopology.EVENTS_EXCHANGE,
                RabbitTopology.RK_USER_FOLLOWED,
                event
        );
        log.info("Evento {} publicado | eventId={} | actorId={} | recipientId={}",
                RabbitTopology.RK_USER_FOLLOWED,
                event.eventId(),
                event.actorId(),
                event.recipientId());
    }
}
