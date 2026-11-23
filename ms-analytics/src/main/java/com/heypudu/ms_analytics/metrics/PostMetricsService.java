package com.heypudu.ms_analytics.metrics;

import com.heypudu.ms_analytics.dto.PostStatsResponse;
import com.heypudu.ms_analytics.messaging.PostEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas de publicaciones en memoria, actualizadas con {@code heypudu.posts.events}.
 *
 * <p>Deduplica por {@code eventId} para que un reenvío del productor no cuente
 * dos veces, igual que {@link RoomMetricsService} con las salas.</p>
 */
@Service
public class PostMetricsService {

    private final Map<UUID, Boolean> processedEvents = new ConcurrentHashMap<>();
    private final AtomicLong created = new AtomicLong();
    private final AtomicLong deleted = new AtomicLong();
    private volatile Instant updatedAt = Instant.now();

    /**
     * Aplica un evento a las métricas.
     *
     * @return {@code false} si el evento ya se había procesado o es inválido
     */
    public boolean apply(PostEvent event) {
        if (event == null || event.eventId() == null || event.postId() == null
                || event.eventType() == null) {
            return false;
        }
        if (processedEvents.putIfAbsent(event.eventId(), Boolean.TRUE) != null) {
            return false;
        }

        switch (event.eventType()) {
            case POST_CREATED -> created.incrementAndGet();
            case POST_DELETED -> deleted.incrementAndGet();
        }
        updatedAt = Instant.now();
        return true;
    }

    public PostStatsResponse stats() {
        long createdTotal = created.get();
        long deletedTotal = deleted.get();
        return new PostStatsResponse(
                createdTotal,
                deletedTotal,
                Math.max(0, createdTotal - deletedTotal),
                updatedAt);
    }
}
