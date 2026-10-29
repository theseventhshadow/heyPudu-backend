package com.heypudu.ms_posts.service.impl;

import com.heypudu.ms_posts.dto.request.CreatePostReportRequest;
import com.heypudu.ms_posts.dto.response.AuthorReportCountResponse;
import com.heypudu.ms_posts.dto.response.PostReportResponse;
import com.heypudu.ms_posts.exception.BusinessException;
import com.heypudu.ms_posts.exception.ResourceNotFoundException;
import com.heypudu.ms_posts.model.Post;
import com.heypudu.ms_posts.model.PostReport;
import com.heypudu.ms_posts.repository.PostReportRepository;
import com.heypudu.ms_posts.repository.PostRepository;
import com.heypudu.ms_posts.security.CognitoUserExtractor;
import com.heypudu.ms_posts.service.PostReportService;
import com.heypudu.ms_posts.service.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PostReportServiceImpl implements PostReportService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_DISMISSED = "DISMISSED";

    private final PostReportRepository reportRepository;
    private final PostRepository postRepository;
    private final S3Service s3Service;
    private final CognitoUserExtractor cognitoUserExtractor;

    public PostReportServiceImpl(PostReportRepository reportRepository,
                                 PostRepository postRepository,
                                 S3Service s3Service,
                                 CognitoUserExtractor cognitoUserExtractor) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.s3Service = s3Service;
        this.cognitoUserExtractor = cognitoUserExtractor;
    }

    @Override
    @Transactional
    public PostReportResponse report(UUID postId, CreatePostReportRequest request, Jwt jwt) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Publicacion no encontrada con id: " + postId));

        UUID reporterId = cognitoUserExtractor.extractSub(jwt);
        if (reportRepository.existsByPostIdAndReporterIdAndStatus(postId, reporterId, STATUS_OPEN)) {
            throw new BusinessException("Ya reportaste esta publicacion");
        }

        PostReport saved = reportRepository.save(new PostReport(
                postId, reporterId, request.reason(), Instant.now()));
        return toResponse(saved, post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostReportResponse> listOpen(Pageable pageable) {
        return reportRepository.findByStatus(STATUS_OPEN, pageable)
                .map(report -> toResponse(
                        report,
                        postRepository.findById(report.getPostId()).orElse(null)));
    }

    @Override
    @Transactional
    public PostReportResponse updateStatus(UUID reportId, String status, Jwt jwt) {
        if (!STATUS_RESOLVED.equals(status) && !STATUS_DISMISSED.equals(status)) {
            throw new BusinessException("Estado de reporte no valido");
        }
        PostReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reporte no encontrado con id: " + reportId));

        report.setStatus(status);
        report.setResolvedAt(Instant.now());
        report.setResolvedBy(cognitoUserExtractor.extractSub(jwt));

        Post post = postRepository.findById(report.getPostId()).orElse(null);
        return toResponse(report, post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthorReportCountResponse> listReportCountsByAuthor() {
        return reportRepository.countReportsByAuthor().stream()
                .map(row -> new AuthorReportCountResponse(
                        (UUID) row[0],
                        ((Number) row[1]).longValue()))
                .toList();
    }

    private PostReportResponse toResponse(PostReport report, Post post) {        String audioUrl = post != null && post.getAudioKey() != null
                ? s3Service.generateAudioDownloadUrl(post.getAudioKey())
                : null;

        return new PostReportResponse(
                report.getId(),
                report.getPostId(),
                post != null ? post.getTitle() : null,
                post != null ? post.getAuthorUsername() : null,
                audioUrl,
                post != null ? post.getAudioDurationSeconds() : null,
                report.getReporterId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedAt());
    }
}
