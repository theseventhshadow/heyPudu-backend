package com.heypudu.ms_users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "follows",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_follows_follower_followed",
        columnNames = {"follower_id", "followed_id"}
    )
)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "follower_id", nullable = false, updatable = false)
    private UUID followerId;

    @Column(name = "followed_id", nullable = false, updatable = false)
    private UUID followedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Follow() {
    }

    public Follow(UUID followerId, UUID followedId, Instant createdAt) {
        this.followerId = followerId;
        this.followedId = followedId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFollowerId() {
        return followerId;
    }

    public void setFollowerId(UUID followerId) {
        this.followerId = followerId;
    }

    public UUID getFollowedId() {
        return followedId;
    }

    public void setFollowedId(UUID followedId) {
        this.followedId = followedId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}