package com.heypudu.ms_interactions.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comment_reports", uniqueConstraints = @UniqueConstraint(
        name = "uq_comment_reports_comment_reporter",
        columnNames = {"comment_id", "reporter_id"}))
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "comment_id", nullable = false, updatable = false)
    private UUID commentId;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private UUID reporterId;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected CommentReport() {
    }

    public CommentReport(UUID commentId, UUID reporterId, String reason, Instant createdAt) {
        this.commentId = commentId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.status = "OPEN";
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getCommentId() { return commentId; }
    public UUID getReporterId() { return reporterId; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setStatus(String status) { this.status = status; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
