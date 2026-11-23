package com.heypudu.ms_analytics.listener;

import com.heypudu.ms_analytics.audit.LiveEventAuditService;
import com.heypudu.ms_analytics.messaging.LiveRoomEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Segundo consumer group sobre el mismo topico: materializa el historico.
 *
 * <p>A diferencia del listener de metricas (best-effort), aqui no se confirma el
 * offset si la escritura falla: el contenedor reintenta y, al reiniciar, el
 * topico se puede reprocesar desde el principio sin duplicar filas.
 */
@Component
public class LiveAuditListener {

    private static final Logger log = LoggerFactory.getLogger(LiveAuditListener.class);

    private final LiveEventAuditService auditService;

    public LiveAuditListener(LiveEventAuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(
            topics = "${live.events.topic:heypudu.live.events}",
            groupId = "${live.audit.group-id:live-audit-group}")
    public void onEvent(LiveRoomEvent event) {
        if (event == null || event.eventId() == null || event.roomId() == null
                || event.eventType() == null) {
            // No se reintenta: el mensaje malformado va directo al DLT.
            throw new IllegalArgumentException("Evento de auditoria invalido: falta eventId, roomId o eventType");
        }
        boolean stored = auditService.record(event);
        if (stored) {
            log.debug("Evento auditado | type={} | roomId={} | eventId={}",
                    event.eventType(), event.roomId(), event.eventId());
        } else {
            log.debug("Evento ya auditado | eventId={}", event.eventId());
        }
    }
}
