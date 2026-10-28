package com.heypudu.ms_interactions.service;

import com.heypudu.ms_interactions.dto.request.CreateCommentReportRequest;
import com.heypudu.ms_interactions.dto.response.CommentReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;

public interface CommentReportService {
    CommentReportResponse report(UUID commentId, CreateCommentReportRequest request, Jwt jwt);
    Page<CommentReportResponse> listOpen(Pageable pageable);
    CommentReportResponse updateStatus(UUID reportId, String status);
}
