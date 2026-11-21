package com.heypudu.ms_live.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "live_rooms")
public class LiveRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "creator_id", nullable = false, updatable = false)
    private UUID creatorId;

    @Column(name = "creator_cognito_sub", nullable = false, updatable = false)
    private UUID creatorCognitoSub;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LiveRoomStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LiveRoom() {
    }

    public LiveRoom(UUID creatorId, UUID creatorCognitoSub, String title, String description) {
        this.creatorId = creatorId;
        this.creatorCognitoSub = creatorCognitoSub;
        this.title = title;
        this.description = description;
        this.status = LiveRoomStatus.LIVE;
        Instant now = Instant.now();
        this.startedAt = now;
        this.createdAt = now;
    }

    public void close() {
        if (status == LiveRoomStatus.CLOSED) {
            return;
        }
        status = LiveRoomStatus.CLOSED;
        endedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCreatorId() { return creatorId; }
    public UUID getCreatorCognitoSub() { return creatorCognitoSub; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LiveRoomStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
