package com.maiu.erp.modules.identity.domain.repository;


import java.util.*;

import com.maiu.erp.modules.identity.domain.model.Role;

public interface RoleRepository {
    Role save(Role role);

    List<Role> findAll();

    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);
}
