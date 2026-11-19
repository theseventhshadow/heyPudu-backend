package com.heypudu.ms_analytics.listener;

import com.heypudu.ms_analytics.messaging.LiveRoomEvent;
import com.heypudu.ms_analytics.metrics.RoomMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class LiveEventListener {

    private static final Logger log = LoggerFactory.getLogger(LiveEventListener.class);

    private final RoomMetricsService metricsService;

    public LiveEventListener(RoomMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @KafkaListener(topics = "${live.events.topic:heypudu.live.events}")
    public void onEvent(LiveRoomEvent event, Acknowledgment acknowledgment) {
        try {
            boolean applied = metricsService.apply(event);
            if (!applied) {
                log.debug("Evento ignorado por duplicado o invalido | eventId={}",
                        event != null ? event.eventId() : null);
            }
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            // Métricas best-effort: se registra y se confirma para no bloquear la partición.
            log.error("No se pudo procesar el evento | eventId={}",
                    event != null ? event.eventId() : null, ex);
            acknowledgment.acknowledge();
        }
    }
}
