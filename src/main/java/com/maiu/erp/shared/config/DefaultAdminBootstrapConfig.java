package com.maiu.erp.shared.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.model.User;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;
import com.maiu.erp.modules.identity.application.service.DefaultRoleService;

@Configuration
public class DefaultAdminBootstrapConfig {
    private static final Logger log = LoggerFactory.getLogger(DefaultAdminBootstrapConfig.class);

    @Bean
    CommandLineRunner seedDefaultAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DefaultRoleService defaultRoleService,
            com.maiu.erp.modules.identity.application.service.DefaultTenantService defaultTenantService,
            @Value("${app.seed.default-admin.enabled:false}") boolean enabled,
            @Value("${app.seed.default-admin.name:Default Admin}") String adminName,
            @Value("${app.seed.default-admin.email:admin@maiu.local}") String adminEmail,
            @Value("${app.seed.default-admin.password:admin123}") String adminPassword) {
        return args -> {
            if (!enabled) {
                log.info("Default admin seeding is disabled");
                return;
            }

            String normalizedEmail = adminEmail.trim().toLowerCase();
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                log.info("Default admin seeding skipped because user already exists: {}", normalizedEmail);
                return;
            }

            log.info("Seeding default admin user: {}", normalizedEmail);

            Role userRole = defaultRoleService.getOrCreateUserRole();
            Role adminRole = defaultRoleService.getOrCreateAdminRole();

            User adminUser = new User(
                    adminName,
                    normalizedEmail,
                    passwordEncoder.encode(adminPassword),
                    Set.of(userRole, adminRole));
            adminUser.setEmailVerified(true);
            adminUser.setTenantId(defaultTenantService.getDefaultTenantId());

            userRepository.save(adminUser);
            log.info("Default admin user created successfully: {}", normalizedEmail);
        };
    }
}
