package com.heypudu.ms_posts.controller;

import com.heypudu.ms_posts.dto.request.CounterUpdateRequest;
import com.heypudu.ms_posts.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/posts")
public class InternalPostController {

    private final PostService postService;

    public InternalPostController(PostService postService) {
        this.postService = postService;
    }

    @PatchMapping("/{postId}/counters")
    public ResponseEntity<Void> updateCounters(
            @PathVariable UUID postId,
            @Valid @RequestBody CounterUpdateRequest request) {

        postService.updateCounters(postId, request.likeDelta(), request.commentDelta());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> anonymizeByAuthor(@PathVariable UUID userId) {
        postService.anonymizeByAuthorId(userId);
        return ResponseEntity.noContent().build();
    }
}