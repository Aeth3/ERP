package com.maiu.erp.shared.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.maiu.erp.modules.identity.application.service.DefaultRoleService;

@Configuration
public class RoleBootstrapConfig {

    @Bean
    CommandLineRunner seedDefaultRoles(DefaultRoleService defaultRoleService) {
        return args -> {
            defaultRoleService.getOrCreateUserRole();
            defaultRoleService.getOrCreateAdminRole();
            defaultRoleService.getOrCreateViewerRole();
            defaultRoleService.getOrCreateProcurementRole();
            defaultRoleService.getOrCreateWarehouseRole();
            defaultRoleService.getOrCreateProjectManagerRole();
        };
    }
}
