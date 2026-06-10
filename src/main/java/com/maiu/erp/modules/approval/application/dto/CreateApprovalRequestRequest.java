package com.maiu.erp.modules.approval.application.dto;

public record CreateApprovalRequestRequest(
        String requestedBy,
        String remarks) {
}
