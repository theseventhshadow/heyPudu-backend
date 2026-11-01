package com.heypudu.ms_users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "type", nullable = false, length = 40)
    private String type;

    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "comment_id")
    private UUID commentId;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notification() {
    }

    public Notification(UUID eventId,
                        UUID recipientId,
                        UUID actorId,
                        String type,
                        UUID postId,
                        UUID commentId,
                        String message,
                        Instant createdAt) {
        this.eventId = eventId;
        this.recipientId = recipientId;
        this.actorId = actorId;
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
        this.message = message;
        this.read = false;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getType() {
        return type;
    }

    public UUID getPostId() {
        return postId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markAsRead() {
        this.read = true;
    }
}
