package com.maiu.erp.modules.approval.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestStatus;
import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalRequestStatus status;

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false)
    private String targetLabel;

    @Column(nullable = false)
    private String requestedBy;

    private String remarks;

    @Column(nullable = false)
    private Instant requestedAt;

    private String approvedBy;

    private Instant approvedAt;

    private String rejectedBy;

    private Instant rejectedAt;

    private String rejectionReason;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ApprovalRequestType getType() {
        return type;
    }

    public void setType(ApprovalRequestType type) {
        this.type = type;
    }

    public ApprovalRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalRequestStatus status) {
        this.status = status;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
