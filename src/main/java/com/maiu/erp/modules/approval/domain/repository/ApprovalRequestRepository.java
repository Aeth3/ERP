package com.maiu.erp.modules.approval.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestStatus;
import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestType;
import com.maiu.erp.modules.approval.domain.model.ApprovalRequest;

public interface ApprovalRequestRepository {
    ApprovalRequest save(ApprovalRequest approvalRequest);

    Optional<ApprovalRequest> findById(UUID id);

    List<ApprovalRequest> findAll();

    List<ApprovalRequest> findByTargetId(UUID targetId);

    Optional<ApprovalRequest> findFirstByTypeAndTargetIdAndStatus(
            ApprovalRequestType type,
            UUID targetId,
            ApprovalRequestStatus status);
}
