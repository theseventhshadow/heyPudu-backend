package com.heypudu.ms_users.controller;

import com.heypudu.ms_users.dto.response.FollowResponse;
import com.heypudu.ms_users.dto.response.UserSummaryResponse;
import com.heypudu.ms_users.service.FollowService;
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
@RequestMapping("/api/v1/users")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{userId}/follow")
    public ResponseEntity<FollowResponse> follow(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(followService.follow(userId, jwt));
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {

        followService.unfollow(userId, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UserSummaryResponse>> listFollowers(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {

        return ResponseEntity.ok(followService.listFollowers(userId, pageable));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UserSummaryResponse>> listFollowing(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {

        return ResponseEntity.ok(followService.listFollowing(userId, pageable));
    }
}