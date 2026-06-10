package com.maiu.erp.modules.approval.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestStatus;
import com.maiu.erp.modules.approval.domain.enums.ApprovalRequestType;
import com.maiu.erp.modules.approval.domain.model.ApprovalRequest;
import com.maiu.erp.modules.approval.domain.repository.ApprovalRequestRepository;
import com.maiu.erp.modules.approval.infrastructure.persistence.entity.ApprovalRequestEntity;
import com.maiu.erp.modules.approval.infrastructure.persistence.jpa.ApprovalRequestJpaRepository;

@Repository
public class ApprovalRequestRepositoryImpl implements ApprovalRequestRepository {
    private final ApprovalRequestJpaRepository jpaRepository;

    public ApprovalRequestRepositoryImpl(ApprovalRequestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ApprovalRequest save(ApprovalRequest approvalRequest) {
        return toDomain(jpaRepository.save(toEntity(approvalRequest)));
    }

    @Override
    public Optional<ApprovalRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ApprovalRequest> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<ApprovalRequest> findByTargetId(UUID targetId) {
        return jpaRepository.findByTargetId(targetId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ApprovalRequest> findFirstByTypeAndTargetIdAndStatus(
            ApprovalRequestType type,
            UUID targetId,
            ApprovalRequestStatus status) {
        return jpaRepository.findFirstByTypeAndTargetIdAndStatusOrderByRequestedAtDesc(type, targetId, status)
                .map(this::toDomain);
    }

    private ApprovalRequest toDomain(ApprovalRequestEntity entity) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setId(entity.getId());
        approvalRequest.setType(entity.getType());
        approvalRequest.setStatus(entity.getStatus());
        approvalRequest.setTargetId(entity.getTargetId());
        approvalRequest.setTargetLabel(entity.getTargetLabel());
        approvalRequest.setRequestedBy(entity.getRequestedBy());
        approvalRequest.setRemarks(entity.getRemarks());
        approvalRequest.setRequestedAt(entity.getRequestedAt());
        approvalRequest.setApprovedBy(entity.getApprovedBy());
        approvalRequest.setApprovedAt(entity.getApprovedAt());
        approvalRequest.setRejectedBy(entity.getRejectedBy());
        approvalRequest.setRejectedAt(entity.getRejectedAt());
        approvalRequest.setRejectionReason(entity.getRejectionReason());
        return approvalRequest;
    }

    private ApprovalRequestEntity toEntity(ApprovalRequest approvalRequest) {
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setId(approvalRequest.getId());
        entity.setType(approvalRequest.getType());
        entity.setStatus(approvalRequest.getStatus());
        entity.setTargetId(approvalRequest.getTargetId());
        entity.setTargetLabel(approvalRequest.getTargetLabel());
        entity.setRequestedBy(approvalRequest.getRequestedBy());
        entity.setRemarks(approvalRequest.getRemarks());
        entity.setRequestedAt(approvalRequest.getRequestedAt());
        entity.setApprovedBy(approvalRequest.getApprovedBy());
        entity.setApprovedAt(approvalRequest.getApprovedAt());
        entity.setRejectedBy(approvalRequest.getRejectedBy());
        entity.setRejectedAt(approvalRequest.getRejectedAt());
        entity.setRejectionReason(approvalRequest.getRejectionReason());
        return entity;
    }
}
