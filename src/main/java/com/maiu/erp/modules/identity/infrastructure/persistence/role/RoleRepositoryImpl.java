package com.maiu.erp.modules.identity.infrastructure.persistence.role;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.identity.domain.repository.RoleRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.maiu.erp.modules.identity.domain.model.Role;

@Repository
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleJpaRepository jpaRepository;

    public RoleRepositoryImpl(RoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity;
        if (role.getId() != null) {
            entity = jpaRepository.findById(role.getId()).orElse(new RoleEntity());
        } else {
            entity = jpaRepository.findByNameIgnoreCase(role.getName()).orElse(new RoleEntity());
        }
        entity.setName(role.getName());
        entity.setPermissions(new LinkedHashSet<>(role.getPermissions()));
        RoleEntity saved = jpaRepository.save(entity);
        return new Role(saved.getId(), saved.getName(), new LinkedHashSet<>(saved.getPermissions()));
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream().map(e -> {
            return new Role(e.getId(), e.getName(), new LinkedHashSet<>(e.getPermissions()));
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Role> findById(Long id) {
        return jpaRepository.findById(id).map(e -> {
            return new Role(e.getId(), e.getName(), new LinkedHashSet<>(e.getPermissions()));
        });
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepository.findByNameIgnoreCase(name)
                .map(e -> new Role(e.getId(), e.getName(), new LinkedHashSet<>(e.getPermissions())));
    }
}
