package com.heypudu.ms_analytics.listener;

import com.heypudu.ms_analytics.messaging.PostEvent;
import com.heypudu.ms_analytics.metrics.PostMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del tópico {@code heypudu.posts.events}.
 *
 * <p>Usa su propio grupo ({@code posts-analytics-group}) y su propio contenedor
 * porque el DTO es distinto al de las salas. Los fallos se reintentan y, si
 * persisten, el mensaje va a {@code heypudu.posts.events.DLT}.</p>
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
    public void onEvent(PostEvent event) {
        if (event == null || event.eventId() == null || event.postId() == null
                || event.eventType() == null) {
            // No se reintenta: el mensaje malformado va directo al DLT.
            throw new IllegalArgumentException("Evento de publicacion invalido: falta eventId, postId o eventType");
        }
        boolean applied = metricsService.apply(event);
        if (!applied) {
            log.debug("Evento de publicacion ignorado por duplicado | eventId={}", event.eventId());
        }
    }
}
