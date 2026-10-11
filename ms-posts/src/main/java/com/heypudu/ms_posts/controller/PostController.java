package com.heypudu.ms_posts.controller;

import com.heypudu.ms_posts.dto.request.CreatePostRequest;
import com.heypudu.ms_posts.dto.request.PresignedUrlRequest;
import com.heypudu.ms_posts.dto.request.UpdatePostRequest;
import com.heypudu.ms_posts.dto.response.PostResponse;
import com.heypudu.ms_posts.dto.response.PresignedUrlResponse;
import com.heypudu.ms_posts.service.PostService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PostResponse response = postService.createPost(request, jwt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {

        PostResponse response = postService.getPost(postId, jwt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Page<PostResponse>> getMyPosts(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<PostResponse> response = postService.getMyPosts(jwt, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<Page<PostResponse>> getPostsByAuthor(
            @PathVariable UUID authorId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<PostResponse> response = postService.getPostsByAuthor(authorId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PostResponse response = postService.updatePost(postId, request, jwt);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {

        postService.deletePost(postId, jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/audio/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generateAudioUploadUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PresignedUrlResponse response = postService.generateAudioUploadUrl(request, jwt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/covers/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generateCoverUploadUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PresignedUrlResponse response = postService.generateCoverUploadUrl(request, jwt);
        return ResponseEntity.ok(response);
    }
}