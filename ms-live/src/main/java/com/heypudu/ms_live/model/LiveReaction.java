package com.heypudu.ms_live.model;

import com.heypudu.ms_live.messaging.ReactionType;
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
@Table(name = "live_reactions")
public class LiveReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "room_id", nullable = false, updatable = false)
    private UUID roomId;

    @Column(name = "actor_cognito_sub", nullable = false, updatable = false)
    private UUID actorCognitoSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 20)
    private ReactionType reactionType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LiveReaction() {
    }

    public LiveReaction(UUID roomId, UUID actorCognitoSub, ReactionType reactionType) {
        this.roomId = roomId;
        this.actorCognitoSub = actorCognitoSub;
        this.reactionType = reactionType;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getActorCognitoSub() { return actorCognitoSub; }
    public ReactionType getReactionType() { return reactionType; }
    public Instant getCreatedAt() { return createdAt; }
}
