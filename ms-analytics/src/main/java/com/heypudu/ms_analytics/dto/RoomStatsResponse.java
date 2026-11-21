package com.heypudu.ms_analytics.dto;

import java.time.Instant;

public record RoomStatsResponse(
        long activeListeners,
        long totalReactions,
        long reactionsPerMinute,
        Instant updatedAt
) {
}
