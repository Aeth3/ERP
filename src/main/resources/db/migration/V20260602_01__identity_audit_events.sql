CREATE TABLE identity_audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(80) NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_identifier VARCHAR(255) NOT NULL,
    details TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_identity_audit_events_created_at
    ON identity_audit_events (created_at DESC);

CREATE INDEX idx_identity_audit_events_target
    ON identity_audit_events (target_type, target_identifier);
