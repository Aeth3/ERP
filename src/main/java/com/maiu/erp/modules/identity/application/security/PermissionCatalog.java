package com.maiu.erp.modules.identity.application.security;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PermissionCatalog {
    private PermissionCatalog() {
    }

    public static final String IDENTITY_USER_READ = "identity.user.read";
    public static final String IDENTITY_USER_MANAGE = "identity.user.manage";
    public static final String IDENTITY_ROLE_READ = "identity.role.read";
    public static final String IDENTITY_ROLE_MANAGE = "identity.role.manage";
    public static final String IDENTITY_AUDIT_READ = "identity.audit.read";

    public static final String INVENTORY_READ = "inventory.read";
    public static final String PROCUREMENT_MANAGE = "inventory.procurement.manage";
    public static final String MASTER_DATA_MANAGE = "inventory.master-data.manage";
    public static final String WAREHOUSE_MANAGE = "inventory.warehouse.manage";
    public static final String SALES_MANAGE = "inventory.sales.manage";
    public static final String STOCK_ADJUST_MANAGE = "inventory.stock-adjust.manage";
    public static final String STOCK_TRANSFER_MANAGE = "inventory.stock-transfer.manage";
    public static final String MATERIAL_ISSUE_MANAGE = "inventory.material-issue.manage";

    public static final String PROJECT_READ = "project.read";
    public static final String PROJECT_MANAGE = "project.manage";
    public static final String PROJECT_BUDGET_MANAGE = "project.budget.manage";
    public static final String PROJECT_REPORT_READ = "project.report.read";
    public static final String APPROVAL_READ = "approval.read";
    public static final String APPROVAL_MANAGE = "approval.manage";

    public static final String UAT_SEED_MANAGE = "uat.seed.manage";

    public static Set<String> allPermissions() {
        return Set.of(
                IDENTITY_USER_READ,
                IDENTITY_USER_MANAGE,
                IDENTITY_ROLE_READ,
                IDENTITY_ROLE_MANAGE,
                IDENTITY_AUDIT_READ,
                INVENTORY_READ,
                PROCUREMENT_MANAGE,
                MASTER_DATA_MANAGE,
                WAREHOUSE_MANAGE,
                SALES_MANAGE,
                STOCK_ADJUST_MANAGE,
                STOCK_TRANSFER_MANAGE,
                MATERIAL_ISSUE_MANAGE,
                PROJECT_READ,
                PROJECT_MANAGE,
                PROJECT_BUDGET_MANAGE,
                PROJECT_REPORT_READ,
                APPROVAL_READ,
                APPROVAL_MANAGE,
                UAT_SEED_MANAGE);
    }

    public static Set<String> userPermissions() {
        return Set.of();
    }

    public static Set<String> adminPermissions() {
        return allPermissions();
    }

    public static Set<String> viewerPermissions() {
        return Set.of(
                INVENTORY_READ,
                PROJECT_READ,
                PROJECT_REPORT_READ);
    }

    public static Set<String> procurementPermissions() {
        return Set.of(
                INVENTORY_READ,
                PROCUREMENT_MANAGE,
                MASTER_DATA_MANAGE,
                APPROVAL_READ,
                PROJECT_READ,
                PROJECT_REPORT_READ);
    }

    public static Set<String> warehousePermissions() {
        return Set.of(
                INVENTORY_READ,
                WAREHOUSE_MANAGE,
                SALES_MANAGE,
                STOCK_ADJUST_MANAGE,
                STOCK_TRANSFER_MANAGE,
                MATERIAL_ISSUE_MANAGE,
                PROJECT_READ,
                PROJECT_REPORT_READ);
    }

    public static Set<String> projectManagerPermissions() {
        return Set.of(
                INVENTORY_READ,
                PROJECT_READ,
                PROJECT_MANAGE,
                PROJECT_BUDGET_MANAGE,
                APPROVAL_READ,
                PROJECT_REPORT_READ);
    }

    public static Set<String> normalize(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (permission == null) {
                continue;
            }
            String value = permission.trim().toLowerCase();
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return Set.copyOf(normalized);
    }
}
