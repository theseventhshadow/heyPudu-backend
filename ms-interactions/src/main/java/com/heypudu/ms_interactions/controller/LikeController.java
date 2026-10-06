package com.heypudu.ms_interactions.controller;

import com.heypudu.ms_interactions.dto.response.LikeCountResponse;
import com.heypudu.ms_interactions.dto.response.LikeResponse;
import com.heypudu.ms_interactions.service.LikeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<LikeResponse> likePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {

        LikeResponse response = likeService.likePost(postId, jwt);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/posts/{postId}/likes")
    public ResponseEntity<Void> unlikePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {

        likeService.unlikePost(postId, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{postId}/likes")
    public ResponseEntity<Page<LikeResponse>> listLikesByPost(
            @PathVariable UUID postId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<LikeResponse> response = likeService.listLikesByPost(postId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/{postId}/likes/count")
    public ResponseEntity<LikeCountResponse> countLikesByPost(@PathVariable UUID postId) {
        LikeCountResponse response = likeService.countLikesByPost(postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/me/likes")
    public ResponseEntity<Page<LikeResponse>> listMyLikes(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<LikeResponse> response = likeService.listMyLikes(jwt, pageable);
        return ResponseEntity.ok(response);
    }
}