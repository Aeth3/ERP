package com.maiu.erp.modules.identity.application.dto;

import java.time.Instant;

public class IdentityAuditEventDto {
    private final Long id;
    private final String eventType;
    private final String actorEmail;
    private final String targetType;
    private final String targetIdentifier;
    private final String details;
    private final Instant createdAt;

    public IdentityAuditEventDto(
            Long id,
            String eventType,
            String actorEmail,
            String targetType,
            String targetIdentifier,
            String details,
            Instant createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.actorEmail = actorEmail;
        this.targetType = targetType;
        this.targetIdentifier = targetIdentifier;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
