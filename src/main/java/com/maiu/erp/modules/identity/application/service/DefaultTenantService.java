package com.maiu.erp.modules.identity.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultTenantService {

    private final Long defaultTenantId;

    public DefaultTenantService(@Value("${app.default-tenant-id:1}") Long defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public Long getDefaultTenantId() {
        return defaultTenantId;
    }
}
