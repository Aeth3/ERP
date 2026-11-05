package com.maiu.erp.modules.approval.application.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestStatus;
import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestType;
import com.maiu.erp.modules.approval.domain.model.ApprovalRequest;
import com.maiu.erp.modules.approval.domain.repository.ApprovalRequestRepository;
import com.maiu.erp.modules.identity.application.security.PermissionCatalog;
import com.maiu.erp.modules.inventory.application.service.PurchaseOrderService;
import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.project.application.service.ProjectService;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.shared.exception.BadRequestException;
import com.maiu.erp.shared.exception.NotFoundException;

import jakarta.transaction.Transactional;

@Service
public class ApprovalWorkflowService {
    private final ApprovalRequestRepository approvalRequestRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final ProjectService projectService;
    private final boolean allowSelfApproval;

    public ApprovalWorkflowService(
            ApprovalRequestRepository approvalRequestRepository,
            PurchaseOrderService purchaseOrderService,
            ProjectService projectService,
            @Value("${app.approval.allow-self-approval:false}") boolean allowSelfApproval) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.projectService = projectService;
        this.allowSelfApproval = allowSelfApproval;
    }

    @PreAuthorize("hasAuthority('" + PermissionCatalog.APPROVAL_READ + "')")
    public List<ApprovalRequest> getApprovalRequests(String status, String type, UUID targetId) {
        return approvalRequestRepository.findAll().stream()
                .filter(request -> matchesStatus(request, status))
                .filter(request -> matchesType(request, type))
                .filter(request -> targetId == null || targetId.equals(request.getTargetId()))
                .sorted(Comparator.comparing(ApprovalRequest::getRequestedAt).reversed())
                .toList();
    }

    public List<ApprovalRequest> getApprovalRequestsForTarget(UUID targetId) {
        return approvalRequestRepository.findByTargetId(targetId).stream()
                .sorted(Comparator.comparing(ApprovalRequest::getRequestedAt).reversed())
                .toList();
    }

    @Transactional
    public ApprovalRequest requestPurchaseOrderApproval(UUID purchaseOrderId, String remarks) {
        PurchaseOrder purchaseOrder = purchaseOrderService.getPurchaseOrderById(purchaseOrderId);
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BadRequestException("Only draft purchase orders can request approval");
        }
        if (purchaseOrderService.getPurchaseOrderItems(purchaseOrderId).isEmpty()) {
            throw new BadRequestException("Purchase order has no items");
        }

        ensureNoPendingRequest(ApprovalRequestType.PURCHASE_ORDER_APPROVAL, purchaseOrderId);
        return approvalRequestRepository.save(buildPendingRequest(
                ApprovalRequestType.PURCHASE_ORDER_APPROVAL,
                purchaseOrderId,
                purchaseOrder.getPoNumber(),
                remarks));
    }

    @Transactional
    public ApprovalRequest requestProjectActivation(UUID projectId, String remarks) {
        Project project = projectService.getProjectById(projectId);
        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BadRequestException("Only draft projects can request activation approval");
        }

        ensureNoPendingRequest(ApprovalRequestType.PROJECT_ACTIVATION, projectId);
        return approvalRequestRepository.save(buildPendingRequest(
                ApprovalRequestType.PROJECT_ACTIVATION,
                projectId,
                project.getProjectCode() + " - " + project.getProjectName(),
                remarks));
    }

    @Transactional
    @PreAuthorize("hasAuthority('" + PermissionCatalog.APPROVAL_MANAGE + "')")
    public ApprovalRequest approveApprovalRequest(UUID approvalRequestId) {
        ApprovalRequest approvalRequest = getPendingApprovalRequest(approvalRequestId);
        String approvedBy = currentActor();
        if (!allowSelfApproval && approvedBy.equalsIgnoreCase(approvalRequest.getRequestedBy())) {
            throw new BadRequestException("You cannot approve a request you raised");
        }

        if (approvalRequest.getType() == ApprovalRequestType.PURCHASE_ORDER_APPROVAL) {
            purchaseOrderService.finalizePurchaseOrderApproval(approvalRequest.getTargetId());
        } else if (approvalRequest.getType() == ApprovalRequestType.PROJECT_ACTIVATION) {
            projectService.finalizeProjectActivation(approvalRequest.getTargetId());
        } else {
            throw new BadRequestException("Unsupported approval type");
        }

        approvalRequest.setStatus(ApprovalRequestStatus.APPROVED);
        approvalRequest.setApprovedBy(approvedBy);
        approvalRequest.setApprovedAt(Instant.now());
        approvalRequest.setRejectedBy(null);
        approvalRequest.setRejectedAt(null);
        approvalRequest.setRejectionReason(null);
        return approvalRequestRepository.save(approvalRequest);
    }

    @Transactional
    @PreAuthorize("hasAuthority('" + PermissionCatalog.APPROVAL_MANAGE + "')")
    public ApprovalRequest rejectApprovalRequest(UUID approvalRequestId, String rejectionReason) {
        ApprovalRequest approvalRequest = getPendingApprovalRequest(approvalRequestId);
        approvalRequest.setStatus(ApprovalRequestStatus.REJECTED);
        approvalRequest.setRejectedBy(currentActor());
        approvalRequest.setRejectedAt(Instant.now());
        approvalRequest.setRejectionReason(normalizeRequired(rejectionReason, "rejectionReason is required"));
        approvalRequest.setApprovedBy(null);
        approvalRequest.setApprovedAt(null);
        return approvalRequestRepository.save(approvalRequest);
    }

    /**
     * Closes the pending request for a target that was cancelled, so it does not linger in the
     * approval queue. The rejection fields record who closed it and why.
     */
    @Transactional
    public void cancelPendingRequest(ApprovalRequestType type, UUID targetId, String reason) {
        approvalRequestRepository.findFirstByTypeAndTargetIdAndStatus(type, targetId, ApprovalRequestStatus.PENDING)
                .ifPresent(approvalRequest -> {
                    approvalRequest.setStatus(ApprovalRequestStatus.CANCELLED);
                    approvalRequest.setRejectedBy(currentActor());
                    approvalRequest.setRejectedAt(Instant.now());
                    approvalRequest.setRejectionReason(reason);
                    approvalRequestRepository.save(approvalRequest);
                });
    }

    public ApprovalRequest getPendingApprovalRequestByTarget(ApprovalRequestType type, UUID targetId) {
        return approvalRequestRepository.findFirstByTypeAndTargetIdAndStatus(type, targetId, ApprovalRequestStatus.PENDING)
                .orElseThrow(() -> new NotFoundException("Approval request not found"));
    }

    private ApprovalRequest getPendingApprovalRequest(UUID approvalRequestId) {
        ApprovalRequest approvalRequest = approvalRequestRepository.findById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Approval request not found"));
        if (approvalRequest.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new BadRequestException("Approval request is not pending");
        }
        return approvalRequest;
    }

    private ApprovalRequest buildPendingRequest(
            ApprovalRequestType type,
            UUID targetId,
            String targetLabel,
            String remarks) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setType(type);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequest.setTargetId(targetId);
        approvalRequest.setTargetLabel(targetLabel);
        approvalRequest.setRequestedBy(currentActor());
        approvalRequest.setRemarks(trimToNull(remarks));
        approvalRequest.setRequestedAt(Instant.now());
        return approvalRequest;
    }

    /** The authenticated user's login name; approval actions are never attributed to a client-supplied name. */
    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new BadRequestException("An authenticated user is required for approval actions");
        }
        return authentication.getName();
    }

    private void ensureNoPendingRequest(ApprovalRequestType type, UUID targetId) {
        if (approvalRequestRepository.findFirstByTypeAndTargetIdAndStatus(type, targetId, ApprovalRequestStatus.PENDING).isPresent()) {
            throw new BadRequestException("A pending approval request already exists");
        }
    }

    private boolean matchesStatus(ApprovalRequest approvalRequest, String status) {
        return status == null
                || status.isBlank()
                || approvalRequest.getStatus().name().equalsIgnoreCase(status);
    }

    private boolean matchesType(ApprovalRequest approvalRequest, String type) {
        return type == null
                || type.isBlank()
                || approvalRequest.getType().name().equalsIgnoreCase(type);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
