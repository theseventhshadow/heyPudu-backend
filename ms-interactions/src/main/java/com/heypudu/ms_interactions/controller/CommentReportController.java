package com.heypudu.ms_interactions.controller;

import com.heypudu.ms_interactions.dto.request.CreateCommentReportRequest;
import com.heypudu.ms_interactions.dto.response.CommentReportResponse;
import com.heypudu.ms_interactions.service.CommentReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CommentReportController {
    private final CommentReportService service;

    public CommentReportController(CommentReportService service) {
        this.service = service;
    }

    @PostMapping("/comments/{commentId}/reports")
    public ResponseEntity<CommentReportResponse> report(
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateCommentReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.report(commentId, request, jwt));
    }

    @GetMapping("/admin/comment-reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CommentReportResponse>> listOpen(
            @PageableDefault(size = 20, sort = "createdAt,asc") Pageable pageable) {
        return ResponseEntity.ok(service.listOpen(pageable));
    }

    @PutMapping("/admin/comment-reports/{reportId}/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommentReportResponse> updateStatus(
            @PathVariable UUID reportId,
            @PathVariable String status) {
        return ResponseEntity.ok(service.updateStatus(reportId, status.toUpperCase()));
    }
}
