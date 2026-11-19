package com.heypudu.ms_analytics.metrics;

import java.time.Instant;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas en memoria de una sala. Pensado para una única instancia de
 * {@code ms-analytics} en la demo.
 */
public class RoomMetrics {

    private static final long WINDOW_SECONDS = 60;

    private final Set<UUID> activeListeners = ConcurrentHashMap.newKeySet();
    private final AtomicLong totalReactions = new AtomicLong();
    private final Deque<Instant> reactionTimestamps = new ConcurrentLinkedDeque<>();
    private volatile Instant updatedAt = Instant.now();

    public void listenerJoined(UUID listenerCognitoSub) {
        activeListeners.add(listenerCognitoSub);
        touch();
    }

    public void listenerLeft(UUID listenerCognitoSub) {
        activeListeners.remove(listenerCognitoSub);
        touch();
    }

    public void reaction(Instant occurredAt) {
        totalReactions.incrementAndGet();
        reactionTimestamps.addLast(occurredAt);
        prune(occurredAt);
        touch();
    }

    public long activeListenerCount() {
        return activeListeners.size();
    }

    public long totalReactionCount() {
        return totalReactions.get();
    }

    public long reactionsLastMinute(Instant now) {
        prune(now);
        return reactionTimestamps.size();
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private void prune(Instant now) {
        Instant threshold = now.minusSeconds(WINDOW_SECONDS);
        Instant oldest = reactionTimestamps.peekFirst();
        while (oldest != null && oldest.isBefore(threshold)) {
            reactionTimestamps.pollFirst();
            oldest = reactionTimestamps.peekFirst();
        }
    }
}
