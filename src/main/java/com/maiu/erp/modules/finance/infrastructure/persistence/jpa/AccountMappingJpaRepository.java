package com.maiu.erp.modules.finance.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maiu.erp.modules.finance.infrastructure.persistence.entity.AccountMappingEntity;

public interface AccountMappingJpaRepository extends JpaRepository<AccountMappingEntity, String> {
}
