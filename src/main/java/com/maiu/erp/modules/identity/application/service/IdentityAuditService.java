package com.maiu.erp.modules.identity.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maiu.erp.modules.identity.application.dto.IdentityAuditEventDto;
import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.model.User;
import com.maiu.erp.modules.identity.infrastructure.persistence.audit.IdentityAuditEventEntity;
import com.maiu.erp.modules.identity.infrastructure.persistence.audit.IdentityAuditEventJpaRepository;

@Service
public class IdentityAuditService {
    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final Logger log = LoggerFactory.getLogger(IdentityAuditService.class);

    private final IdentityAuditEventJpaRepository auditRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IdentityAuditService(
            IdentityAuditEventJpaRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void recordUserCreated(User user) {
        record(
                "USER_CREATED",
                "USER",
                user.getEmail(),
                Map.of(
                        "userId", user.getId(),
                        "name", user.getName(),
                        "roles", extractRoleNames(user.getRoles())));
    }

    public void recordUserDeleted(User user) {
        record(
                "USER_DELETED",
                "USER",
                user.getEmail(),
                Map.of(
                        "userId", user.getId(),
                        "name", user.getName(),
                        "roles", extractRoleNames(user.getRoles())));
    }

    public void recordUserRolesUpdated(String email, Set<String> beforeRoles, Set<String> afterRoles) {
        record(
                "USER_ROLES_UPDATED",
                "USER",
                email,
                Map.of(
                        "beforeRoles", beforeRoles,
                        "afterRoles", afterRoles));
    }

    public void recordRoleCreated(Role role) {
        record(
                "ROLE_CREATED",
                "ROLE",
                role.getName(),
                Map.of(
                        "roleId", role.getId(),
                        "permissions", role.getPermissions()));
    }

    public void recordRolePermissionsUpdated(Role role, Set<String> beforePermissions, Set<String> afterPermissions) {
        record(
                "ROLE_PERMISSIONS_UPDATED",
                "ROLE",
                role.getName(),
                Map.of(
                        "roleId", role.getId(),
                        "beforePermissions", beforePermissions,
                        "afterPermissions", afterPermissions));
    }

    public List<IdentityAuditEventDto> getRecentEvents() {
        return auditRepository.findTop100ByOrderByCreatedAtDescIdDesc().stream()
                .map(event -> new IdentityAuditEventDto(
                        event.getId(),
                        event.getEventType(),
                        event.getActorEmail(),
                        event.getTargetType(),
                        event.getTargetIdentifier(),
                        event.getDetails(),
                        event.getCreatedAt()))
                .toList();
    }

    private void record(String eventType, String targetType, String targetIdentifier, Map<String, Object> details) {
        try {
            IdentityAuditEventEntity entity = new IdentityAuditEventEntity();
            entity.setEventType(eventType);
            entity.setActorEmail(resolveActorEmail());
            entity.setTargetType(targetType);
            entity.setTargetIdentifier(targetIdentifier);
            entity.setDetails(serialize(details));
            entity.setCreatedAt(Instant.now());
            auditRepository.save(entity);
        } catch (Exception exception) {
            log.warn("Identity audit recording failed for event {} target {}. Continuing without blocking request.",
                    eventType,
                    targetIdentifier,
                    exception);
        }
    }

    private String resolveActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return SYSTEM_ACTOR;
        }
        return authentication.getName();
    }

    private String serialize(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to serialize identity audit details", exception);
        }
    }

    private Set<String> extractRoleNames(Set<Role> roles) {
        return roles == null ? Set.of() : roles.stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
    }
}
