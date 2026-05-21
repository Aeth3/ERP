package com.maiu.erp.modules.identity.infrastructure.persistence.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<UserEntity> findByEmail(@Param("email") String email);
}
