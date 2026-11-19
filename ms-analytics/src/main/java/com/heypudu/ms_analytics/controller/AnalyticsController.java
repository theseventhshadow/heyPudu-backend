package com.heypudu.ms_analytics.controller;

import com.heypudu.ms_analytics.dto.RoomStatsResponse;
import com.heypudu.ms_analytics.metrics.RoomMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/rooms")
public class AnalyticsController {

    private final RoomMetricsService metricsService;

    public AnalyticsController(RoomMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/{roomId}/stats")
    public ResponseEntity<RoomStatsResponse> getRoomStats(@PathVariable UUID roomId) {
        return ResponseEntity.ok(metricsService.stats(roomId));
    }
}
