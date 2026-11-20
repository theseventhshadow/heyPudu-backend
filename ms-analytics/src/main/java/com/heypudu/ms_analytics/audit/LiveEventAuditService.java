package com.heypudu.ms_analytics.audit;

import com.heypudu.ms_analytics.messaging.LiveRoomEvent;
import com.heypudu.ms_analytics.model.LiveEventAudit;
import com.heypudu.ms_analytics.repository.LiveEventAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste los eventos del topico. La PK es el {@code eventId}, por lo que
 * reprocesar el topico completo (o reintentar un mensaje) no duplica filas.
 */
@Service
public class LiveEventAuditService {

    private static final Logger log = LoggerFactory.getLogger(LiveEventAuditService.class);

    private final LiveEventAuditRepository repository;

    public LiveEventAuditService(LiveEventAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean record(LiveRoomEvent event) {
        if (event == null || event.eventId() == null || event.roomId() == null || event.eventType() == null) {
            log.warn("Evento de auditoria invalido, se descarta");
            return false;
        }
        if (repository.existsById(event.eventId())) {
            return false;
        }
        try {
            repository.save(new LiveEventAudit(event));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            // Carrera entre consumidores o reproceso: la PK ya existe.
            return false;
        }
    }
}
