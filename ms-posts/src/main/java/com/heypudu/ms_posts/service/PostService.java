package com.heypudu.ms_posts.service;

import com.heypudu.ms_posts.dto.request.CreatePostRequest;
import com.heypudu.ms_posts.dto.request.PresignedUrlRequest;
import com.heypudu.ms_posts.dto.request.UpdatePostRequest;
import com.heypudu.ms_posts.dto.response.PostResponse;
import com.heypudu.ms_posts.dto.response.PresignedUrlResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface PostService {

    PostResponse createPost(CreatePostRequest request, Jwt jwt);

    PostResponse getPost(UUID postId, Jwt jwt);

    Page<PostResponse> getMyPosts(Jwt jwt, Pageable pageable);

    Page<PostResponse> getPostsByAuthor(UUID authorId, Pageable pageable);

    PostResponse updatePost(UUID postId, UpdatePostRequest request, Jwt jwt);

    void deletePost(UUID postId, Jwt jwt);

    PresignedUrlResponse generateAudioUploadUrl(PresignedUrlRequest request, Jwt jwt);

    PresignedUrlResponse generateCoverUploadUrl(PresignedUrlRequest request, Jwt jwt);

    void updateCounters(UUID postId, int likeDelta, int commentDelta);

    void anonymizeByAuthorId(UUID authorId);
}