package com.maiu.erp.modules.identity.infrastructure.persistence.user;

import com.maiu.erp.modules.identity.domain.model.User;



public class UserMapper {

    public static User toDomain(UserEntity entity) {
        User user = new User();
        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setPassword(entity.getPassword());
        user.setTenantId(entity.getTenantId());
        user.setEmailVerified(entity.isEmailVerified());
        return user;
    }

    public static UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail());
        entity.setPassword(domain.getPassword());
        entity.setTenantId(domain.getTenantId());
        entity.setEmailVerified(domain.isEmailVerified());
        return entity;
    }
}
