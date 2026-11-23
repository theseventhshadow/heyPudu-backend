package com.heypudu.ms_analytics.listener;

import com.heypudu.ms_analytics.messaging.PostEvent;
import com.heypudu.ms_analytics.metrics.PostMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumidor del tópico {@code heypudu.posts.events}.
 *
 * <p>Usa su propio grupo ({@code posts-analytics-group}) y su propio contenedor
 * porque el DTO es distinto al de las salas: dos consumidores con grupos
 * separados pueden leer el mismo clúster sin compartir offsets.</p>
 */
@Component
public class PostEventListener {

    private static final Logger log = LoggerFactory.getLogger(PostEventListener.class);

    private final PostMetricsService metricsService;

    public PostEventListener(PostMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @KafkaListener(
            topics = "${posts.events.topic:heypudu.posts.events}",
            containerFactory = "postsListenerContainerFactory")
    public void onEvent(PostEvent event, Acknowledgment acknowledgment) {
        try {
            boolean applied = metricsService.apply(event);
            if (!applied) {
                log.debug("Evento de publicacion ignorado por duplicado o invalido | eventId={}",
                        event != null ? event.eventId() : null);
            }
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            // Métricas best-effort: se registra y se confirma para no bloquear la partición.
            log.error("No se pudo procesar el evento de publicacion | eventId={}",
                    event != null ? event.eventId() : null, ex);
            acknowledgment.acknowledge();
        }
    }
}
