package com.sivacrackers.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One line of the activity log. Written once, never changed. Never contains passwords or tokens. */
@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    @Column(name = "actor_name", length = 100, updatable = false)
    private String actorName;

    @Column(nullable = false, length = 60, updatable = false)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 40, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Column(length = 1000, updatable = false)
    private String details;

    protected AuditEntry() {
        // for JPA
    }

    public AuditEntry(Instant occurredAt, Long actorId, String actorName, String action, String entityType,
                      Long entityId, String details) {
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public Long getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getDetails() { return details; }
}
