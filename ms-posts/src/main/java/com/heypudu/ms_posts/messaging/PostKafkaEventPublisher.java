package com.heypudu.ms_posts.messaging;

import com.heypudu.ms_posts.model.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica los eventos de publicación en el tópico {@code heypudu.posts.events}.
 *
 * <p>Es best-effort: si Kafka no responde se registra el error y la operación
 * del usuario continúa. No se confunde con {@link PostEventPublisher}, que
 * publica comandos por RabbitMQ.</p>
 */
@Component
public class PostKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PostKafkaEventPublisher.class);
    private static final int SCHEMA_VERSION = 1;

    private final KafkaTemplate<String, PostEvent> kafkaTemplate;
    private final String topic;

    public PostKafkaEventPublisher(KafkaTemplate<String, PostEvent> kafkaTemplate,
                                   @Value("${posts.events.topic:heypudu.posts.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void postCreated(Post post) {
        publish(new PostEvent(
                UUID.randomUUID(),
                PostEventType.POST_CREATED,
                post.getId(),
                post.getAuthorId(),
                post.getType(),
                Instant.now(),
                SCHEMA_VERSION));
    }

    public void postDeleted(UUID postId, UUID authorCognitoSub, String postType) {
        publish(new PostEvent(
                UUID.randomUUID(),
                PostEventType.POST_DELETED,
                postId,
                authorCognitoSub,
                postType,
                Instant.now(),
                SCHEMA_VERSION));
    }

    private void publish(PostEvent event) {
        try {
            kafkaTemplate.send(topic, event.postId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("No se pudo publicar el evento {} de la publicacion {}",
                                    event.eventType(), event.postId(), ex);
                        } else if (result != null) {
                            log.info("Evento {} publicado | postId={} | partition={} | offset={}",
                                    event.eventType(),
                                    event.postId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException ex) {
            log.error("Error enviando el evento {} de la publicacion {}",
                    event.eventType(), event.postId(), ex);
        }
    }
}
