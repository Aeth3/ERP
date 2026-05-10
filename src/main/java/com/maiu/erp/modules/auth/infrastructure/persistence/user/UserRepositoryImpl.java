package com.maiu.erp.modules.auth.infrastructure.persistence.user;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.auth.domain.model.Role;
import com.maiu.erp.modules.auth.domain.model.User;
import com.maiu.erp.modules.auth.domain.repository.UserRepository;
import com.maiu.erp.modules.auth.infrastructure.persistence.role.RoleEntity;


@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryImpl(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {

        UserEntity entity;

        if (user.getId() != null) {
            entity = jpaRepository.findById(user.getId())
                    .orElse(new UserEntity());
        } else {
            entity = new UserEntity();
        }

        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());

        // ✅ Map roles properly
        entity.setRoles(
                user.getRoles().stream()
                        .map(r -> {
                            RoleEntity re = new RoleEntity();
                            re.setId(r.getId());
                            return re;
                        })
                        .collect(Collectors.toSet()));

        UserEntity saved = jpaRepository.save(entity);

        return toDomain(saved);
    }

    private User toDomain(UserEntity e) {
        Set<Role> roles = e.getRoles().stream()
                .map(r -> new Role(r.getId(), r.getName(), r.getPermissions()))
                .collect(Collectors.toSet());

        User user = new User(
                e.getName(),
                e.getEmail(),
                e.getPassword(),
                roles);

        user.setId(e.getId());
        user.setTenantId(e.getTenantId());

        return user;
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public void deleteUser(Long id) {
        if (!jpaRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }

        jpaRepository.deleteById(id);
    }
}
