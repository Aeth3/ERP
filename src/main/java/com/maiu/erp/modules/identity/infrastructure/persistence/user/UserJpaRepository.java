package com.maiu.erp.modules.identity.infrastructure.persistence.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    @Override
    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    java.util.List<UserEntity> findAll();

    @Override
    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    Optional<UserEntity> findById(Long id);

    @EntityGraph(attributePaths = { "roles", "roles.permissions" })
    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.email = :email")
    Optional<UserEntity> findByEmail(@Param("email") String email);
}
