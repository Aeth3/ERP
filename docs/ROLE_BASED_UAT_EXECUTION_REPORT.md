# Role-Based UAT Execution Report

## Purpose

Use this document to record the actual results of a live UAT cycle using the seeded roles and scenarios.

Related inputs:

- [`ROLE_BASED_UAT_CHECKLIST.md`](./ROLE_BASED_UAT_CHECKLIST.md)
- [`UAT_SEED_PLAN.md`](./UAT_SEED_PLAN.md)
- [`IDENTITY_PERMISSION_MATRIX.md`](./IDENTITY_PERMISSION_MATRIX.md)

## UAT Cycle Info

| Field | Value |
| --- | --- |
| UAT Cycle Name | |
| Environment | |
| Backend Version | |
| Frontend Version | |
| Date Started | |
| Date Completed | |
| Facilitator | |
| Client / Team | |

## Seed Confirmation

| Item | Status | Notes |
| --- | --- | --- |
| UAT users seeded | | |
| Master data seeded | | |
| `PRJ-UAT-001` available | | |
| Budget lines available | | |
| Login verified for all test accounts | | |

## Role Results Summary

| Role | Tester | Result | Critical Issues | Notes |
| --- | --- | --- | --- | --- |
| `ADMIN` | | | | |
| `PROJECT_MANAGER` | | | | |
| `PROCUREMENT` | | | | |
| `WAREHOUSE` | | | | |
| `VIEWER` | | | | |

## Detailed Execution

### `ADMIN`

| Check | Pass / Fail | Notes |
| --- | --- | --- |
| User list visible | | |
| Role assignment works | | |
| Identity audit log visible | | |
| Manual UAT seed action works if enabled | | |
| Project creation and budget maintenance work | | |

### `PROJECT_MANAGER`

| Check | Pass / Fail | Notes |
| --- | --- | --- |
| Project create/edit works | | |
| Budget lines work | | |
| Project reporting tabs load | | |
| Restricted admin pages blocked | | |

### `PROCUREMENT`

| Check | Pass / Fail | Notes |
| --- | --- | --- |
| Purchase orders work | | |
| Supplier/customer/category/product/unit maintenance works | | |
| Restricted warehouse tools blocked | | |
| Project-linked procurement visibility correct | | |

### `WAREHOUSE`

| Check | Pass / Fail | Notes |
| --- | --- | --- |
| Stock inquiry works | | |
| Stock adjustment works | | |
| Stock transfer works | | |
| Sales order flow works | | |
| Material issue / return / reversal work | | |

### `VIEWER`

| Check | Pass / Fail | Notes |
| --- | --- | --- |
| Read-only pages visible | | |
| Write buttons hidden | | |
| Restricted routes blocked safely | | |

## Identity / Permission Checks

| Check | Pass / Fail | Notes |
| --- | --- | --- |
| Default roles have expected permissions | | |
| `/roles/permissions/catalog` returns expected data | | |
| `/roles/{id}/permissions` works for admin | | |
| Identity audit captures user creation | | |
| Identity audit captures role assignment | | |
| Identity audit captures role permission updates | | |
| Identity audit captures user deletion | | |

## Defects

| ID | Severity | Role | Area | Summary | Status |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

## Final Recommendation

Choose one:

- `PASS`
- `PASS WITH CONDITIONS`
- `FAIL`

Summary:

-

Sign-off:

- Business Lead:
- Technical Lead:
- Date:
