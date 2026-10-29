package com.heypudu.ms_interactions.controller;

import com.heypudu.ms_interactions.service.CommentService;
import com.heypudu.ms_interactions.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/posts")
public class InternalInteractionController {

    private final LikeService likeService;
    private final CommentService commentService;

    public InternalInteractionController(LikeService likeService, CommentService commentService) {
        this.likeService = likeService;
        this.commentService = commentService;
    }

    @DeleteMapping("/{postId}/interactions")
    public ResponseEntity<Void> deleteAllByPost(@PathVariable UUID postId) {
        likeService.deleteAllByPost(postId);
        commentService.deleteAllByPost(postId);
        return ResponseEntity.noContent().build();
    }
}
