package com.maiu.erp.modules.identity.infrastructure.persistence.audit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityAuditEventJpaRepository extends JpaRepository<IdentityAuditEventEntity, Long> {
    List<IdentityAuditEventEntity> findTop100ByOrderByCreatedAtDescIdDesc();
}
