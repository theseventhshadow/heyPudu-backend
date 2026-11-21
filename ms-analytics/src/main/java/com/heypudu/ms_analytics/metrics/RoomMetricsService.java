package com.heypudu.ms_analytics.metrics;

import com.heypudu.ms_analytics.dto.RoomStatsResponse;
import com.heypudu.ms_analytics.messaging.LiveRoomEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomMetricsService {

    /**
     * Caché acotada de {@code eventId} ya procesados para evitar doble conteo
     * tras un rebalanceo o un reinicio del consumidor.
     */
    private static final int MAX_PROCESSED_EVENTS = 20_000;

    private final Map<UUID, RoomMetrics> rooms = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> processedEvents = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, Boolean> eldest) {
                    return size() > MAX_PROCESSED_EVENTS;
                }
            });

    /**
     * Aplica un evento a las métricas de su sala.
     *
     * @return {@code false} si el evento ya se había procesado o es inválido
     */
    public boolean apply(LiveRoomEvent event) {
        if (event == null || event.eventId() == null || event.roomId() == null
                || event.eventType() == null) {
            return false;
        }
        if (processedEvents.putIfAbsent(event.eventId(), Boolean.TRUE) != null) {
            return false;
        }

        RoomMetrics metrics = rooms.computeIfAbsent(event.roomId(), id -> new RoomMetrics());

        switch (event.eventType()) {
            case ROOM_CREATED -> {
                // La entrada de métricas ya quedó creada.
            }
            case LISTENER_JOINED -> {
                if (event.actorCognitoSub() != null) {
                    metrics.listenerJoined(event.actorCognitoSub());
                }
            }
            case LISTENER_LEFT -> {
                if (event.actorCognitoSub() != null) {
                    metrics.listenerLeft(event.actorCognitoSub());
                }
            }
            case REACTION -> metrics.reaction(
                    event.occurredAt() != null ? event.occurredAt() : Instant.now());
        }
        return true;
    }

    public RoomStatsResponse stats(UUID roomId) {
        RoomMetrics metrics = rooms.get(roomId);
        if (metrics == null) {
            return new RoomStatsResponse(0, 0, 0, Instant.now());
        }
        Instant now = Instant.now();
        return new RoomStatsResponse(
                metrics.activeListenerCount(),
                metrics.totalReactionCount(),
                metrics.reactionsLastMinute(now),
                metrics.updatedAt());
    }
}
