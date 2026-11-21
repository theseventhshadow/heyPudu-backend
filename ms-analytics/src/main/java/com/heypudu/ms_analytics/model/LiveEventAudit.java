package com.heypudu.ms_analytics.model;

import com.heypudu.ms_analytics.messaging.LiveRoomEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Copia persistida de un evento del topico. La PK es el {@code eventId}, asi que
 * un reproceso completo del topico no duplica filas.
 */
@Entity
@Table(name = "live_events_audit")
public class LiveEventAudit {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "room_id", nullable = false, updatable = false)
    private UUID roomId;

    @Column(name = "creator_cognito_sub")
    private UUID creatorCognitoSub;

    @Column(name = "actor_cognito_sub")
    private UUID actorCognitoSub;

    @Column(name = "reaction_type", length = 20)
    private String reactionType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "ingested_at", nullable = false, insertable = false, updatable = false)
    private Instant ingestedAt;

    protected LiveEventAudit() {
    }

    public LiveEventAudit(LiveRoomEvent event) {
        this.eventId = event.eventId();
        this.eventType = event.eventType().name();
        this.roomId = event.roomId();
        this.creatorCognitoSub = event.creatorCognitoSub();
        this.actorCognitoSub = event.actorCognitoSub();
        this.reactionType = event.reactionType() != null ? event.reactionType().name() : null;
        this.schemaVersion = event.schemaVersion();
        this.occurredAt = event.occurredAt();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
