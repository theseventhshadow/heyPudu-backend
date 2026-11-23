package com.heypudu.ms_analytics.listener;

import com.heypudu.ms_analytics.messaging.LiveRoomEvent;
import com.heypudu.ms_analytics.metrics.RoomMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Métricas en tiempo real de las salas.
 *
 * <p>Ya no confirma el offset a mano: el {@code DefaultErrorHandler} reintenta
 * los fallos transitorios y manda a {@code heypudu.live.events.DLT} los
 * mensajes inválidos o los que agoten los reintentos.</p>
 */
@Component
public class LiveEventListener {

    private static final Logger log = LoggerFactory.getLogger(LiveEventListener.class);

    private final RoomMetricsService metricsService;

    public LiveEventListener(RoomMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @KafkaListener(topics = "${live.events.topic:heypudu.live.events}")
    public void onEvent(LiveRoomEvent event) {
        if (event == null || event.eventId() == null || event.roomId() == null
                || event.eventType() == null) {
            // No se reintenta: el mensaje malformado va directo al DLT.
            throw new IllegalArgumentException("Evento de sala invalido: falta eventId, roomId o eventType");
        }
        boolean applied = metricsService.apply(event);
        if (!applied) {
            log.debug("Evento ignorado por duplicado | eventId={}", event.eventId());
        }
    }
}
