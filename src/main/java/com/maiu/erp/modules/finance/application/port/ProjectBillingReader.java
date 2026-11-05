package com.maiu.erp.modules.finance.application.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Finance's window into project contract terms, implemented by the project module. */
public interface ProjectBillingReader {

    Optional<ProjectSnapshot> find(UUID projectId);

    record ProjectSnapshot(
            UUID id,
            String projectCode,
            String projectName,
            UUID customerId,
            String status,
            BigDecimal contractAmount,
            BigDecimal retentionRate) {
    }
}
