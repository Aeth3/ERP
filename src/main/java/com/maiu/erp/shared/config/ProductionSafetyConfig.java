package com.maiu.erp.shared.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ProductionSafetyConfig {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");
    private static final Set<String> INSECURE_JWT_VALUES = new HashSet<>(Arrays.asList(
            "",
            "change-me",
            "change-me-dev",
            "default",
            "secret"));

    @Bean
    CommandLineRunner productionSafetyGuard(
            Environment environment,
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${spring.jpa.hibernate.ddl-auto:}") String ddlAuto,
            @Value("${app.seed.default-admin.enabled:false}") boolean defaultAdminEnabled,
            @Value("${app.seed.uat.enabled:false}") boolean uatSeedEnabled,
            @Value("${app.seed.uat.manual-enabled:false}") boolean uatManualEnabled,
            @Value("${spring.flyway.enabled:true}") boolean flywayEnabled) {
        return args -> {
            boolean productionProfileActive = Arrays.stream(environment.getActiveProfiles())
                    .anyMatch(PRODUCTION_PROFILES::contains);

            if (!productionProfileActive) {
                return;
            }

            String normalizedJwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
            String normalizedDdlAuto = ddlAuto == null ? "" : ddlAuto.trim().toLowerCase();

            if (INSECURE_JWT_VALUES.contains(normalizedJwtSecret)) {
                throw new IllegalStateException("Refusing to start with an insecure JWT secret in production");
            }

            if (!normalizedDdlAuto.equals("validate") && !normalizedDdlAuto.equals("none")) {
                throw new IllegalStateException("Refusing to start with spring.jpa.hibernate.ddl-auto=" + ddlAuto
                        + " in production. Use validate or none.");
            }

            if (defaultAdminEnabled) {
                throw new IllegalStateException("Refusing to start with default admin seeding enabled in production");
            }

            if (uatSeedEnabled) {
                throw new IllegalStateException("Refusing to start with UAT baseline seeding enabled in production");
            }

            if (uatManualEnabled) {
                throw new IllegalStateException("Refusing to start with manual UAT seeding enabled in production");
            }

            if (!flywayEnabled) {
                throw new IllegalStateException("Refusing to start with Flyway disabled in production");
            }
        };
    }
}
