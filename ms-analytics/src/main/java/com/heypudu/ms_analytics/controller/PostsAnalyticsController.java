package com.heypudu.ms_analytics.controller;

import com.heypudu.ms_analytics.dto.PostStatsResponse;
import com.heypudu.ms_analytics.metrics.PostMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Métricas de publicaciones calculadas desde el tópico {@code heypudu.posts.events}.
 */
@RestController
@RequestMapping("/api/v1/analytics/posts")
public class PostsAnalyticsController {

    private final PostMetricsService metricsService;

    public PostsAnalyticsController(PostMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<PostStatsResponse> getPostStats() {
        return ResponseEntity.ok(metricsService.stats());
    }
}
