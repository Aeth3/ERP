package com.maiu.erp.modules.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.maiu.erp.modules.identity.domain.model.User;



public interface UserRepository {
    User save(User user);

    List<User> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    void deleteUser(Long id);
}
