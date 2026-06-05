package com.maiu.erp.modules.identity.infrastructure.persistence.role;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {
    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findByNameIgnoreCase(String name);

    @Override
    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "permissions")
    List<RoleEntity> findAll();
}
