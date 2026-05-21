package com.maiu.erp.modules.identity.infrastructure.persistence.role;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByNameIgnoreCase(String name);
}
