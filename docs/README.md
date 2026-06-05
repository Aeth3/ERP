# ERP Docs Index

Use this folder as the single home for ERP planning and operating documents.

## Available Docs

- [`CONSTRUCTION_CLIENT_PROCESS_FLOW.md`](./CONSTRUCTION_CLIENT_PROCESS_FLOW.md)
  Construction-specific end-to-end workflow for your client.

- [`INVENTORY_FLOW.md`](./INVENTORY_FLOW.md)
  Current inventory and project-linked stock behavior, rules, and APIs.

- [`PROJECT_MODULE_PLAN.md`](./PROJECT_MODULE_PLAN.md)
  Hardening and completion plan for the project module.

- [`MODULE_IMPLEMENTATION_PLAN.md`](./MODULE_IMPLEMENTATION_PLAN.md)
  Recommended module rollout order, starting simple but scalable later.

- [`PRODUCTION_READINESS_CHECKLIST.md`](./PRODUCTION_READINESS_CHECKLIST.md)
  Go-live readiness checklist and current maturity assessment for Inventory and Project modules.

- [`ROLE_BASED_UAT_CHECKLIST.md`](./ROLE_BASED_UAT_CHECKLIST.md)
  Role-by-role validation checklist for `ADMIN`, `PROJECT_MANAGER`, `PROCUREMENT`, `WAREHOUSE`, and `VIEWER`.

- [`UAT_SEED_PLAN.md`](./UAT_SEED_PLAN.md)
  Repeatable UAT users, master data, project, and transaction seed plan.

- [`IDENTITY_PERMISSION_MATRIX.md`](./IDENTITY_PERMISSION_MATRIX.md)
  Current permission catalog, default role mapping, and next-step path toward finer-grained authorization.

- [`ROLE_BASED_UAT_EXECUTION_REPORT.md`](./ROLE_BASED_UAT_EXECUTION_REPORT.md)
  Live UAT recording template for role-by-role execution and sign-off.

- [`ENV_FILE_MAP.md`](./ENV_FILE_MAP.md)
  Environment file split and usage guidance.

- [`SECRET_ROTATION.md`](./SECRET_ROTATION.md)
  Secret rotation checklist and verification steps.

## Recommended Reading Order

If you are reviewing the business/system direction:

1. `CONSTRUCTION_CLIENT_PROCESS_FLOW.md`
2. `MODULE_IMPLEMENTATION_PLAN.md`
3. `PRODUCTION_READINESS_CHECKLIST.md`
4. `ROLE_BASED_UAT_CHECKLIST.md`
5. `UAT_SEED_PLAN.md`
6. `IDENTITY_PERMISSION_MATRIX.md`
7. `ROLE_BASED_UAT_EXECUTION_REPORT.md`
8. `PROJECT_MODULE_PLAN.md`
9. `INVENTORY_FLOW.md`

If you are setting up or deploying the backend:

1. `ENV_FILE_MAP.md`
2. `SECRET_ROTATION.md`
