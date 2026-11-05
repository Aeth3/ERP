package com.maiu.erp.shared.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.maiu.erp.modules.finance.application.service.FinanceSetupService;

@Configuration
public class FinanceBootstrapConfig {

    @Bean
    CommandLineRunner seedFinanceDefaults(FinanceSetupService financeSetupService) {
        return args -> financeSetupService.ensureDefaults();
    }
}
