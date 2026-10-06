package com.heypudu.ms_posts.service;

import com.heypudu.ms_posts.dto.response.PostSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface FeedService {

    Page<PostSummaryResponse> getGeneralFeed(Pageable pageable);

    Page<PostSummaryResponse> getPersonalFeed(Jwt jwt, Pageable pageable);

    Page<PostSummaryResponse> searchPosts(String query, Pageable pageable);
}