package com.maiu.erp.modules.approval.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestStatus;
import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestType;
import com.maiu.erp.modules.approval.infrastructure.persistence.entity.ApprovalRequestEntity;

public interface ApprovalRequestJpaRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
    List<ApprovalRequestEntity> findByTargetId(UUID targetId);

    Optional<ApprovalRequestEntity> findFirstByTypeAndTargetIdAndStatusOrderByRequestedAtDesc(
            ApprovalRequestType type,
            UUID targetId,
            ApprovalRequestStatus status);
}
