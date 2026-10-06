package com.heypudu.ms_interactions.controller;

import com.heypudu.ms_interactions.dto.request.CreateCommentRequest;
import com.heypudu.ms_interactions.dto.response.CommentResponse;
import com.heypudu.ms_interactions.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        CommentResponse response = commentService.createComment(postId, request, jwt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> listCommentsByPost(
            @PathVariable UUID postId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<CommentResponse> response = commentService.listCommentsByPost(postId, pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal Jwt jwt) {

        commentService.deleteComment(commentId, jwt);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/comments/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCommentAsAdmin(@PathVariable UUID commentId) {
        commentService.deleteCommentAsAdmin(commentId);
        return ResponseEntity.noContent().build();
    }
}