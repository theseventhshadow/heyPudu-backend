package com.heypudu.ms_interactions.repository;

import com.heypudu.ms_interactions.model.CommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {
    boolean existsByCommentIdAndReporterId(UUID commentId, UUID reporterId);
    Page<CommentReport> findByStatus(String status, Pageable pageable);
}
