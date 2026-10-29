package com.heypudu.ms_interactions.repository;

import com.heypudu.ms_interactions.model.CommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {
    boolean existsByCommentIdAndReporterIdAndStatus(UUID commentId, UUID reporterId, String status);
    Page<CommentReport> findByStatus(String status, Pageable pageable);

    @Query("""
            SELECT c.userId, COUNT(r.id)
            FROM CommentReport r, Comment c
            WHERE c.id = r.commentId
              AND r.status <> 'DISMISSED'
            GROUP BY c.userId
            """)
    List<Object[]> countReportsByAuthor();
}
