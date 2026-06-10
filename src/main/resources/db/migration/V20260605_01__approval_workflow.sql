CREATE TABLE approval_requests (
    id UUID PRIMARY KEY,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    target_id UUID NOT NULL,
    target_label VARCHAR(255) NOT NULL,
    requested_by VARCHAR(255) NOT NULL,
    remarks TEXT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_by VARCHAR(255) NULL,
    approved_at TIMESTAMP WITH TIME ZONE NULL,
    rejected_by VARCHAR(255) NULL,
    rejected_at TIMESTAMP WITH TIME ZONE NULL,
    rejection_reason TEXT NULL
);

CREATE INDEX idx_approval_requests_target_id ON approval_requests (target_id);
CREATE INDEX idx_approval_requests_status ON approval_requests (status);
CREATE INDEX idx_approval_requests_type_status_target ON approval_requests (type, status, target_id);
