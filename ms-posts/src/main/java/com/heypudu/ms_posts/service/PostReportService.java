package com.heypudu.ms_posts.service;

import com.heypudu.ms_posts.dto.request.CreatePostReportRequest;
import com.heypudu.ms_posts.dto.response.PostReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface PostReportService {

    PostReportResponse report(UUID postId, CreatePostReportRequest request, Jwt jwt);

    Page<PostReportResponse> listOpen(Pageable pageable);

    PostReportResponse updateStatus(UUID reportId, String status, Jwt jwt);
}
