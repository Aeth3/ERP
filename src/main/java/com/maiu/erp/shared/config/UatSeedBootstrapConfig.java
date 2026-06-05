package com.maiu.erp.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.maiu.erp.shared.seed.UatSeedService;

@Configuration
public class UatSeedBootstrapConfig {
    private static final Logger log = LoggerFactory.getLogger(UatSeedBootstrapConfig.class);

    @Bean
    CommandLineRunner seedUatBaseline(
            UatSeedService uatSeedService,
            @Value("${app.seed.uat.enabled:false}") boolean enabled,
            @Value("${app.seed.uat.shared-password:uat123}") String sharedPassword) {
        return args -> {
            if (!enabled) {
                log.info("UAT baseline seeding is disabled");
                return;
            }

            uatSeedService.seed(sharedPassword);
            log.info("UAT baseline seeding completed");
        };
    }
}
