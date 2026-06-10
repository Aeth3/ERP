package com.maiu.erp.modules.approval.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.approval.application.dto.ActionResponse;
import com.maiu.erp.modules.approval.application.dto.ApproveApprovalRequestRequest;
import com.maiu.erp.modules.approval.application.dto.ApprovalRequestDto;
import com.maiu.erp.modules.approval.application.dto.RejectApprovalRequestRequest;
import com.maiu.erp.modules.approval.application.service.ApprovalWorkflowService;
import com.maiu.erp.modules.approval.domain.model.ApprovalRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/approvals")
public class ApprovalWorkflowController {
    private final ApprovalWorkflowService approvalWorkflowService;

    public ApprovalWorkflowController(ApprovalWorkflowService approvalWorkflowService) {
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @GetMapping
    public ResponseEntity<List<ApprovalRequestDto>> getApprovalRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID targetId) {
        return ResponseEntity.ok(approvalWorkflowService.getApprovalRequests(status, type, targetId).stream()
                .map(this::toDto)
                .toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ActionResponse> approveApprovalRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveApprovalRequestRequest request) {
        approvalWorkflowService.approveApprovalRequest(id, request.approvedBy());
        return ResponseEntity.ok(new ActionResponse("Approval request approved successfully"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ActionResponse> rejectApprovalRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectApprovalRequestRequest request) {
        approvalWorkflowService.rejectApprovalRequest(id, request.rejectedBy(), request.rejectionReason());
        return ResponseEntity.ok(new ActionResponse("Approval request rejected successfully"));
    }

    private ApprovalRequestDto toDto(ApprovalRequest approvalRequest) {
        return new ApprovalRequestDto(
                approvalRequest.getId(),
                approvalRequest.getType(),
                approvalRequest.getStatus(),
                approvalRequest.getTargetId(),
                approvalRequest.getTargetLabel(),
                approvalRequest.getRequestedBy(),
                approvalRequest.getRemarks(),
                approvalRequest.getRequestedAt(),
                approvalRequest.getApprovedBy(),
                approvalRequest.getApprovedAt(),
                approvalRequest.getRejectedBy(),
                approvalRequest.getRejectedAt(),
                approvalRequest.getRejectionReason());
    }
}
