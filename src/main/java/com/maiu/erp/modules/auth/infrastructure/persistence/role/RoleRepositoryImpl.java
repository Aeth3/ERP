package com.maiu.erp.modules.auth.infrastructure.persistence.role;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.auth.domain.repository.RoleRepository;

import java.util.*;
import java.util.stream.Collectors;

import com.maiu.erp.modules.auth.domain.model.Role;

@Repository
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleJpaRepository jpaRepository;

    public RoleRepositoryImpl(RoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = new RoleEntity();
        entity.setName(role.getName());
        entity.setPermissions(role.getPermissions());
        RoleEntity saved = jpaRepository.save(entity);
        role.setId(saved.getId());
        return role;
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream().map(e -> {
            return new Role(e.getId(), e.getName(), e.getPermissions());
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Role> findById(Long id) {
        return jpaRepository.findById(id).map(e -> {
            return new Role(e.getId(), e.getName(), e.getPermissions());
        });
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepository.findByNameIgnoreCase(name)
                .map(e -> new Role(e.getId(), e.getName(), e.getPermissions()));
    }
}
