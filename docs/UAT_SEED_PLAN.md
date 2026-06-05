# UAT Seed Plan

## Purpose

This document defines a repeatable seed setup for validating the current `Inventory`, `Project`, and `Identity` modules during UAT.

Use it together with [`ROLE_BASED_UAT_CHECKLIST.md`](./ROLE_BASED_UAT_CHECKLIST.md).

It is intentionally simple enough for first rollout, while still realistic for a construction-client workflow.

Current backend support:

- you can enable baseline seeding with `APP_SEED_UAT_ENABLED=true`
- you can enable manual admin-triggered reseeding with `APP_SEED_UAT_MANUAL_ENABLED=true`
- you can override the shared tester password with `APP_SEED_UAT_SHARED_PASSWORD`
- the bootstrap currently seeds users, master data, the main UAT project, and budget lines
- transactional records like POs, issues, returns, and reversals should still be created through the app during UAT

Manual reseed endpoint:

- `POST /admin/uat/seed`
- requires `ADMIN`
- uses the configured shared password and idempotent seed logic

## Seed Accounts

Prepare these users before UAT starts:

| Name | Email | Role | Purpose |
| --- | --- | --- | --- |
| Default Admin | `admin@maiu.local` | `ADMIN` | Full control, setup, defect recovery |
| Project Lead | `pm.uat@maiu.local` | `PROJECT_MANAGER` | Project creation, budget, status, reporting |
| Procurement Officer | `procurement.uat@maiu.local` | `PROCUREMENT` | Purchase orders, supplier-side corrections |
| Warehouse Officer | `warehouse.uat@maiu.local` | `WAREHOUSE` | Stock operations, site issue, returns, reversals |
| Read Only User | `viewer.uat@maiu.local` | `VIEWER` | Read-only validation |

Recommended password policy for UAT:

- use a shared temporary password that is not used in production
- require password reset before any real rollout
- keep a simple login sheet for testers

## Seed Master Data

Create these master records first.

### Units

- `pcs`
- `bag`
- `roll`

### Categories

- `Cement and Concrete`
- `Electrical`
- `Finishing`

### Warehouses

- `Main Warehouse`
- `Site Buffer Warehouse`

### Suppliers

- `ABC Construction Supply`
- `Metro Electrical Trading`

### Customers

- `Internal Construction Client`

### Products

Use at least these products:

| SKU | Product | Category | Unit | Example Cost |
| --- | --- | --- | --- | --- |
| `CEM-001` | Portland Cement 40kg | Cement and Concrete | bag | 285.00 |
| `REB-010` | Rebar 10mm | Cement and Concrete | pcs | 420.00 |
| `WIRE-250` | Electrical Wire 2.5mm | Electrical | roll | 1650.00 |

## Seed Project

Create one main UAT project:

| Field | Value |
| --- | --- |
| Code | `PRJ-UAT-001` |
| Name | `Two-Storey Residential Build - UAT` |
| Customer | `Internal Construction Client` |
| Status | `ACTIVE` |
| Budget Amount | `500000.00` |

Recommended budget lines:

| Line | Amount |
| --- | --- |
| Civil Works | `220000.00` |
| Electrical Works | `120000.00` |
| Finishing Works | `160000.00` |

## Seed Transaction Set

Prepare transactions in this order so every role has something realistic to validate.

### Purchase Orders

Create these purchase orders:

1. One `DRAFT` project-linked PO for `PRJ-UAT-001`
2. One `APPROVED` project-linked PO for `PRJ-UAT-001`
3. One `RECEIVED` project-linked PO for `PRJ-UAT-001`

Suggested received PO lines:

| Product | Qty | Unit Cost |
| --- | --- | --- |
| Portland Cement 40kg | 100 | 285.00 |
| Rebar 10mm | 40 | 420.00 |

Receive into:

- `Main Warehouse`

### Material Issue

Create one issued transaction from `Main Warehouse` to `PRJ-UAT-001`:

| Product | Qty | Unit Cost Reference |
| --- | --- | --- |
| Portland Cement 40kg | 20 | from received stock |
| Rebar 10mm | 10 | from received stock |

### Material Return

Create one partial material return:

| Product | Qty |
| --- | --- |
| Portland Cement 40kg | 5 |

### Material Reversal

Reverse the remaining quantity for one issue line after the partial return so UAT can validate:

- net issue calculation
- reversal tagging
- movement ledger behavior

### Purchase Return

Create one purchase return from the received project-linked PO:

| Product | Qty |
| --- | --- |
| Rebar 10mm | 2 |

### Sales Order

Create one warehouse-side sales flow for inventory validation:

1. Create one sales order
2. Confirm it against `Main Warehouse`
3. Ship it
4. Create a partial sales return

Suggested shipped line:

| Product | Qty |
| --- | --- |
| Electrical Wire 2.5mm | 2 |

## Minimum UAT Dataset Outcome

By the time seeding is complete, the system should contain:

- 5 test users with role coverage
- 1 active project with budget and budget lines
- 3 products across multiple categories
- 2 warehouses
- purchase orders in multiple lifecycle states
- at least 1 material issue
- at least 1 material return
- at least 1 reversal
- at least 1 purchase return
- at least 1 sales return

## Execution Order

Use this order during setup:

1. Seed users and roles
2. Seed units, categories, suppliers, customers, warehouses, products
3. Create the main UAT project and budget lines
4. Create project-linked purchase orders in staged statuses
5. Receive at least one PO into `Main Warehouse`
6. Create project material issue
7. Create partial material return
8. Reverse remaining issue balance
9. Create purchase return
10. Create sales order, shipment, and sales return

## Reset Guidance

For repeatable UAT cycles:

- keep one named UAT project instead of many random projects
- keep tester emails stable across cycles
- archive or clear incomplete UAT transactions before reseeding
- apply all Flyway migrations before every test cycle
- verify role assignments before opening UAT

If the environment becomes noisy, reseed from clean master data and rebuild only the core scenario above.

## UAT Handoff Notes

Before handing the environment to testers, confirm:

- backend starts cleanly with all migrations applied
- frontend route visibility matches the assigned role
- seeded users can log in
- `PRJ-UAT-001` dashboard loads
- project movements, cost summary, and budget lines display correctly

## Related Docs

- [`ROLE_BASED_UAT_CHECKLIST.md`](./ROLE_BASED_UAT_CHECKLIST.md)
- [`PRODUCTION_READINESS_CHECKLIST.md`](./PRODUCTION_READINESS_CHECKLIST.md)
- [`CONSTRUCTION_CLIENT_PROCESS_FLOW.md`](./CONSTRUCTION_CLIENT_PROCESS_FLOW.md)
