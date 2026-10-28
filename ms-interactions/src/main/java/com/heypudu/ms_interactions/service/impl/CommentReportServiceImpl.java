package com.heypudu.ms_interactions.service.impl;

import com.heypudu.ms_interactions.dto.request.CreateCommentReportRequest;
import com.heypudu.ms_interactions.dto.response.CommentReportResponse;
import com.heypudu.ms_interactions.exception.BusinessException;
import com.heypudu.ms_interactions.exception.ResourceNotFoundException;
import com.heypudu.ms_interactions.model.CommentReport;
import com.heypudu.ms_interactions.repository.CommentReportRepository;
import com.heypudu.ms_interactions.repository.CommentRepository;
import com.heypudu.ms_interactions.security.CognitoUserExtractor;
import com.heypudu.ms_interactions.service.CommentReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class CommentReportServiceImpl implements CommentReportService {
    private final CommentReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final CognitoUserExtractor userExtractor;

    public CommentReportServiceImpl(CommentReportRepository reportRepository,
                                    CommentRepository commentRepository,
                                    CognitoUserExtractor userExtractor) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
        this.userExtractor = userExtractor;
    }

    @Override
    @Transactional
    public CommentReportResponse report(UUID commentId, CreateCommentReportRequest request, Jwt jwt) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comentario no encontrado con id: " + commentId);
        }
        UUID reporterId = userExtractor.extractSub(jwt);
        if (reportRepository.existsByCommentIdAndReporterId(commentId, reporterId)) {
            throw new BusinessException("Ya reportaste este comentario");
        }
        return toResponse(reportRepository.save(new CommentReport(
                commentId, reporterId, request.reason(), Instant.now())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentReportResponse> listOpen(Pageable pageable) {
        return reportRepository.findByStatus("OPEN", pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public CommentReportResponse updateStatus(UUID reportId, String status) {
        if (!status.equals("RESOLVED") && !status.equals("DISMISSED")) {
            throw new BusinessException("Estado de reporte no válido");
        }
        CommentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con id: " + reportId));
        report.setStatus(status);
        report.setResolvedAt(Instant.now());
        return toResponse(report);
    }

    private CommentReportResponse toResponse(CommentReport report) {
        var comment = commentRepository.findById(report.getCommentId()).orElse(null);
        return new CommentReportResponse(report.getId(), report.getCommentId(),
                comment == null ? null : comment.getPostId(),
                comment == null ? null : comment.getContent(),
                report.getReporterId(), report.getReason(), report.getStatus(),
                report.getCreatedAt(), report.getResolvedAt());
    }
}
