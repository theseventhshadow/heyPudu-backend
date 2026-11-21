package com.heypudu.ms_live.messaging;

import com.heypudu.ms_live.model.LiveRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica eventos de sala en Kafka con clave {@code roomId}.
 *
 * <p>La publicación es best-effort: un fallo de Kafka se registra pero nunca
 * interrumpe la operación de la sala (decisión de disponibilidad de la Fase 4).
 */
@Component
public class LiveEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LiveEventPublisher.class);
    private static final int SCHEMA_VERSION = 1;

    private final KafkaTemplate<String, LiveRoomEvent> kafkaTemplate;
    private final String topic;

    public LiveEventPublisher(
            KafkaTemplate<String, LiveRoomEvent> kafkaTemplate,
            @Value("${live.events.topic:heypudu.live.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void roomCreated(LiveRoom room) {
        publish(buildEvent(LiveEventType.ROOM_CREATED, room, room.getCreatorCognitoSub(), null));
    }

    public void listenerJoined(LiveRoom room, UUID listenerCognitoSub) {
        publish(buildEvent(LiveEventType.LISTENER_JOINED, room, listenerCognitoSub, null));
    }

    public void listenerLeft(LiveRoom room, UUID listenerCognitoSub) {
        publish(buildEvent(LiveEventType.LISTENER_LEFT, room, listenerCognitoSub, null));
    }

    public void reaction(LiveRoom room, UUID actorCognitoSub, ReactionType reactionType) {
        publish(buildEvent(LiveEventType.REACTION, room, actorCognitoSub, reactionType));
    }

    private LiveRoomEvent buildEvent(LiveEventType type,
                                     LiveRoom room,
                                     UUID actorCognitoSub,
                                     ReactionType reactionType) {
        return new LiveRoomEvent(
                UUID.randomUUID(),
                type,
                room.getId(),
                room.getCreatorCognitoSub(),
                actorCognitoSub,
                reactionType,
                Instant.now(),
                SCHEMA_VERSION);
    }

    private void publish(LiveRoomEvent event) {
        try {
            kafkaTemplate.send(topic, event.roomId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("No se pudo publicar el evento Kafka | type={} | roomId={} | eventId={}",
                                    event.eventType(), event.roomId(), event.eventId(), ex);
                            return;
                        }
                        log.debug("Evento Kafka publicado | type={} | roomId={} | partition={} | offset={}",
                                event.eventType(),
                                event.roomId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    });
        } catch (Exception ex) {
            // La disponibilidad de la sala no depende de Kafka.
            log.error("Kafka no disponible al publicar | type={} | roomId={} | eventId={}",
                    event.eventType(), event.roomId(), event.eventId(), ex);
        }
    }
}
