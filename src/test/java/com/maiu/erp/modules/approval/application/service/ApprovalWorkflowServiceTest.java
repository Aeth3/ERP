package com.maiu.erp.modules.approval.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestStatus;
import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestType;
import com.maiu.erp.modules.approval.domain.model.ApprovalRequest;
import com.maiu.erp.modules.approval.domain.repository.ApprovalRequestRepository;
import com.maiu.erp.modules.inventory.application.service.PurchaseOrderService;
import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.project.application.service.ProjectService;

class ApprovalWorkflowServiceTest {

    private InMemoryApprovalRequestRepository approvalRequestRepository;
    private PurchaseOrderService purchaseOrderService;
    private ProjectService projectService;
    private UUID purchaseOrderId;

    @BeforeEach
    void setUp() {
        approvalRequestRepository = new InMemoryApprovalRequestRepository();
        purchaseOrderService = Mockito.mock(PurchaseOrderService.class);
        projectService = Mockito.mock(ProjectService.class);
        purchaseOrderId = UUID.randomUUID();

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(purchaseOrderId);
        purchaseOrder.setPoNumber("PO-0001");
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
        when(purchaseOrderService.getPurchaseOrderById(purchaseOrderId)).thenReturn(purchaseOrder);
        when(purchaseOrderService.getPurchaseOrderItems(purchaseOrderId)).thenReturn(List.of(new PurchaseOrderItem()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requesterIsTheAuthenticatedUser() {
        signInAs("buyer@maiu.test");

        ApprovalRequest request = service(false).requestPurchaseOrderApproval(purchaseOrderId, "Urgent");

        assertEquals("buyer@maiu.test", request.getRequestedBy());
        assertEquals(ApprovalRequestStatus.PENDING, request.getStatus());
    }

    @Test
    void approverIsTheAuthenticatedUser() {
        signInAs("buyer@maiu.test");
        ApprovalRequest request = service(false).requestPurchaseOrderApproval(purchaseOrderId, null);

        signInAs("manager@maiu.test");
        ApprovalRequest approved = service(false).approveApprovalRequest(request.getId());

        assertEquals(ApprovalRequestStatus.APPROVED, approved.getStatus());
        assertEquals("manager@maiu.test", approved.getApprovedBy());
        verify(purchaseOrderService).finalizePurchaseOrderApproval(purchaseOrderId);
    }

    @Test
    void requesterCannotApproveOwnRequest() {
        signInAs("buyer@maiu.test");
        ApprovalWorkflowService service = service(false);
        ApprovalRequest request = service.requestPurchaseOrderApproval(purchaseOrderId, null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.approveApprovalRequest(request.getId()));

        assertEquals("You cannot approve a request you raised", exception.getMessage());
        assertEquals(ApprovalRequestStatus.PENDING, approvalRequestRepository.findById(request.getId()).orElseThrow().getStatus());
        verify(purchaseOrderService, never()).finalizePurchaseOrderApproval(purchaseOrderId);
    }

    @Test
    void selfApprovalAllowedWhenConfigured() {
        signInAs("owner@maiu.test");
        ApprovalWorkflowService service = service(true);
        ApprovalRequest request = service.requestPurchaseOrderApproval(purchaseOrderId, null);

        ApprovalRequest approved = service.approveApprovalRequest(request.getId());

        assertEquals(ApprovalRequestStatus.APPROVED, approved.getStatus());
        assertEquals("owner@maiu.test", approved.getApprovedBy());
    }

    @Test
    void approvalActionsRequireAuthenticatedUser() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service(false).requestPurchaseOrderApproval(purchaseOrderId, null));

        assertEquals("An authenticated user is required for approval actions", exception.getMessage());
    }

    @Test
    void cancelPendingRequestClosesItWithReason() {
        signInAs("buyer@maiu.test");
        ApprovalWorkflowService service = service(false);
        ApprovalRequest request = service.requestPurchaseOrderApproval(purchaseOrderId, null);

        service.cancelPendingRequest(ApprovalRequestType.PURCHASE_ORDER_APPROVAL, purchaseOrderId, "Purchase order was cancelled");

        ApprovalRequest cancelled = approvalRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(ApprovalRequestStatus.CANCELLED, cancelled.getStatus());
        assertEquals("buyer@maiu.test", cancelled.getRejectedBy());
        assertEquals("Purchase order was cancelled", cancelled.getRejectionReason());
    }

    @Test
    void cancelPendingRequestIsNoOpWithoutPendingRequest() {
        signInAs("buyer@maiu.test");

        service(false).cancelPendingRequest(ApprovalRequestType.PURCHASE_ORDER_APPROVAL, purchaseOrderId, "Purchase order was cancelled");

        assertEquals(0, approvalRequestRepository.findAll().size());
    }

    private ApprovalWorkflowService service(boolean allowSelfApproval) {
        return new ApprovalWorkflowService(approvalRequestRepository, purchaseOrderService, projectService, allowSelfApproval);
    }

    private static void signInAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(username, null, List.of()));
    }

    private static final class InMemoryApprovalRequestRepository implements ApprovalRequestRepository {
        private final List<ApprovalRequest> requests = new ArrayList<>();

        @Override
        public ApprovalRequest save(ApprovalRequest approvalRequest) {
            if (approvalRequest.getId() == null) {
                approvalRequest.setId(UUID.randomUUID());
            }
            requests.removeIf(existing -> existing.getId().equals(approvalRequest.getId()));
            requests.add(approvalRequest);
            return approvalRequest;
        }

        @Override
        public Optional<ApprovalRequest> findById(UUID id) {
            return requests.stream().filter(request -> request.getId().equals(id)).findFirst();
        }

        @Override
        public List<ApprovalRequest> findAll() {
            return List.copyOf(requests);
        }

        @Override
        public List<ApprovalRequest> findByTargetId(UUID targetId) {
            return requests.stream().filter(request -> request.getTargetId().equals(targetId)).toList();
        }

        @Override
        public Optional<ApprovalRequest> findFirstByTypeAndTargetIdAndStatus(
                ApprovalRequestType type,
                UUID targetId,
                ApprovalRequestStatus status) {
            return requests.stream()
                    .filter(request -> request.getType() == type
                            && request.getTargetId().equals(targetId)
                            && request.getStatus() == status)
                    .findFirst();
        }
    }
}
