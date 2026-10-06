package com.heypudu.ms_posts.controller;

import com.heypudu.ms_posts.dto.response.PostSummaryResponse;
import com.heypudu.ms_posts.service.FeedService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> getGeneralFeed(
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {

        Page<PostSummaryResponse> response = feedService.getGeneralFeed(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PostSummaryResponse>> getPersonalFeed(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {

        Page<PostSummaryResponse> response = feedService.getPersonalFeed(jwt, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PostSummaryResponse>> searchPosts(
            @RequestParam("q") String query,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {

        Page<PostSummaryResponse> response = feedService.searchPosts(query, pageable);
        return ResponseEntity.ok(response);
    }
}