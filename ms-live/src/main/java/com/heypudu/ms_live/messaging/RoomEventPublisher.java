package com.heypudu.ms_live.messaging;

import com.heypudu.ms_live.config.amqp.RabbitTopology;
import com.heypudu.ms_live.model.LiveRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica los eventos y comandos de ciclo de vida de una sala en RabbitMQ.
 *
 * <p>Son best-effort: un fallo de RabbitMQ se registra y no interrumpe la
 * operación de la sala.</p>
 */
@Component
public class RoomEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RoomEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RoomEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRoomCreated(LiveRoom room) {
        RoomCreatedEvent event = new RoomCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                room.getId(),
                room.getCreatorCognitoSub(),
                room.getTitle());
        send(RabbitTopology.EVENTS_EXCHANGE, RabbitTopology.RK_ROOM_CREATED, event);
    }

    public void publishRoomClosed(LiveRoom room) {
        RoomClosedMessage message = new RoomClosedMessage(
                UUID.randomUUID(),
                room.getId(),
                Instant.now());
        send(RabbitTopology.COMMAND_EXCHANGE, RabbitTopology.RK_ROOM_CLOSED, message);
    }

    private void send(String exchange, String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("Mensaje Rabbit publicado | routingKey={} | payload={}", routingKey, payload);
        } catch (Exception ex) {
            // La sala no debe fallar porque RabbitMQ no esté disponible.
            log.error("No se pudo publicar en RabbitMQ | routingKey={}", routingKey, ex);
        }
    }
}
