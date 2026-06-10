package com.maiu.erp.modules.approval.application.dto;

public record RejectApprovalRequestRequest(
        String rejectedBy,
        String rejectionReason) {
}
