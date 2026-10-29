package com.heypudu.ms_posts.controller;

import com.heypudu.ms_posts.dto.request.CreatePostReportRequest;
import com.heypudu.ms_posts.dto.response.AuthorReportCountResponse;
import com.heypudu.ms_posts.dto.response.PostReportResponse;
import com.heypudu.ms_posts.service.PostReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PostReportController {

    private final PostReportService postReportService;

    public PostReportController(PostReportService postReportService) {
        this.postReportService = postReportService;
    }

    @PostMapping("/posts/{postId}/reports")
    public ResponseEntity<PostReportResponse> report(
            @PathVariable UUID postId,
            @Valid @RequestBody CreatePostReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(postReportService.report(postId, request, jwt));
    }

    @GetMapping("/admin/posts/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PostReportResponse>> listOpen(
            @PageableDefault(size = 20, sort = "createdAt,asc") Pageable pageable) {
        return ResponseEntity.ok(postReportService.listOpen(pageable));
    }

    @GetMapping("/internal/posts/reports/counts")
    public ResponseEntity<List<AuthorReportCountResponse>> listReportCounts() {
        return ResponseEntity.ok(postReportService.listReportCountsByAuthor());
    }

    @PutMapping("/admin/posts/reports/{reportId}/{status}")    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostReportResponse> updateStatus(
            @PathVariable UUID reportId,
            @PathVariable String status,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(postReportService.updateStatus(reportId, status.toUpperCase(), jwt));
    }
}
