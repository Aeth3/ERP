# Identity Permission Matrix

## Purpose

This document describes the current permission model behind the `Identity`, `Inventory`, and `Project` modules.

It complements role-based access. Roles still remain the main assignment unit, but permissions now exist as a first-class model for finer control later.

## Current Permission Catalog

### Identity

- `identity.user.read`
- `identity.user.manage`
- `identity.role.read`
- `identity.role.manage`
- `identity.audit.read`

### Inventory

- `inventory.read`
- `inventory.procurement.manage`
- `inventory.master-data.manage`
- `inventory.warehouse.manage`
- `inventory.sales.manage`
- `inventory.stock-adjust.manage`
- `inventory.stock-transfer.manage`
- `inventory.material-issue.manage`

### Project

- `project.read`
- `project.manage`
- `project.budget.manage`
- `project.report.read`

### Environment / UAT

- `uat.seed.manage`

## Default Role Mapping

### `ADMIN`

Has all current permissions.

### `VIEWER`

- `inventory.read`
- `project.read`
- `project.report.read`

### `PROCUREMENT`

- `inventory.read`
- `inventory.procurement.manage`
- `inventory.master-data.manage`
- `project.read`
- `project.report.read`

### `WAREHOUSE`

- `inventory.read`
- `inventory.warehouse.manage`
- `inventory.sales.manage`
- `inventory.stock-adjust.manage`
- `inventory.stock-transfer.manage`
- `inventory.material-issue.manage`
- `project.read`
- `project.report.read`

### `PROJECT_MANAGER`

- `inventory.read`
- `project.read`
- `project.manage`
- `project.budget.manage`
- `project.report.read`

### `USER`

Currently no extra business permissions by default.

## Current Backend Usage

Permissions are now used in the backend for:

- role management service methods
- identity audit access
- admin user delete / role assignment service methods
- manual UAT seed action

Roles are still used heavily in route-level security, so the current system is:

- role-enforced at route level
- permission-capable at service/method level

## Future Migration Path

If you decide to move to a more granular model later, the safest order is:

1. keep current roles as-is
2. continue seeding default permissions onto those roles
3. move critical service methods to permission checks first
4. relax route rules only after UAT confirms permission behavior
5. introduce custom business roles only when needed by real staff structure

## Recommended Next Uses

The best next places to expand permission-based enforcement are:

- project workspace execution actions
- procurement lifecycle actions
- warehouse correction actions
- admin reporting and audit access

## Related Endpoints

- `GET /roles`
- `POST /roles`
- `PUT /roles/{id}/permissions`
- `GET /roles/permissions/catalog`
- `GET /admin/audit/identity`
