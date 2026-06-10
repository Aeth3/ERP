package com.maiu.erp.modules.approval.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestStatus;
import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestType;

public record ApprovalRequestDto(
        UUID id,
        ApprovalRequestType type,
        ApprovalRequestStatus status,
        UUID targetId,
        String targetLabel,
        String requestedBy,
        String remarks,
        Instant requestedAt,
        String approvedBy,
        Instant approvedAt,
        String rejectedBy,
        Instant rejectedAt,
        String rejectionReason) {
}
