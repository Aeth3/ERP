# Project Module Plan

## Purpose

Complete the project module so it can manage project lifecycle, cost visibility, and inventory-linked execution without weakening stock control or auditability.

Monetary values referenced in this document should be treated as Philippine peso (PHP).

## Current Baseline

Already present in the codebase:

- project master data: create, update, list, fetch
- status actions: activate, hold, complete, cancel
- explicit lifecycle transition rules for `DRAFT`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, and `CANCELLED`
- project-linked purchase orders
- material issue posting to projects
- material return posting back from projects
- project cost summary endpoint
- simple project budget amount
- project budget lines with committed-vs-actual cost summary support
- migration support for `projects`, `material_issues`, and project-linked stock movements

This means the next phase is not "start project module", but "finish and harden project operations."

## Recommended Order

### Phase 1: Harden Inventory-to-Project Integration

Goal:
- make project-linked inventory flows trustworthy before expanding features

Tasks:
- persist the warehouse used during sales-order confirmation so ship/cancel cannot act on a different warehouse
- enforce required request fields for project-linked flows such as `performedBy`, `warehouseId`, and `projectId`
- validate purchase-order receiving inputs before stock is posted
- add tests for cross-warehouse reservation misuse, null request fields, and invalid status transitions

Exit criteria:
- all stock-affecting flows are deterministic and reject invalid state changes cleanly
- regression tests cover the main inventory/project edge cases

### Phase 2: Strengthen Project Lifecycle Rules

Goal:
- define and enforce a real project workflow instead of only loose status switching

Tasks:
- confirm allowed transitions between `DRAFT`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, and `CANCELLED`
- block invalid jumps if the business does not allow them
- decide whether completed/cancelled projects may be reopened
- add timestamps or audit fields for lifecycle actions if needed

Exit criteria:
- project state changes match agreed business workflow
- service and controller tests cover all transitions

### Phase 3: Expand Project Commercial Controls

Goal:
- move from simple project records to controlled project execution

Tasks:
- evolve the current project budget amount into budget lines or structured budgets
- track planned vs actual net material cost
- expose variance in the cost summary endpoint
- decide whether purchase-order totals should count as committed cost, actual cost, or both

Exit criteria:
- project cost reports answer budget, committed, issued, and variance questions clearly

### Phase 4: Add Corrections and Returns

Goal:
- support real operational mistakes without manual database fixes

Tasks:
- extend the current material return flow into a fuller issue correction toolkit
- add return/reversal strategy for project-linked purchased and issued stock where needed
- preserve audit trail instead of mutating history silently

Exit criteria:
- accidental issues or wrong quantities can be corrected through supported APIs

### Phase 5: Improve Querying and Reporting

Goal:
- make the module usable for supervisors, PMs, and finance

Tasks:
- filter project costs by date, product, and warehouse
- add project movement history view
- add project dashboard metrics such as total issued quantity, recent issues, and active project count
- consider a per-project inventory ledger endpoint

Exit criteria:
- users can answer project cost and material-usage questions without raw database queries

## Suggested API Additions

- `POST /projects/{id}/reopen` if reopening is part of the workflow
- `POST /projects/{id}/budget-lines`
- `PUT /projects/{id}/budget-lines/{budgetLineId}`
- `DELETE /projects/{id}/budget-lines/{budgetLineId}`
- `GET /projects/{id}/movements`
- `GET /projects/{id}/dashboard`
- `GET /projects/{id}/materials`
- `POST /projects/{id}/material-returns`
- `POST /projects/{id}/issue-reversals`

## Data Model Candidates

- `project_budgets`
- `project_budget_lines`
- `material_returns`
- `material_return_items`
- optional project audit/history table if lifecycle tracking needs to be explicit

## Testing Plan

- unit tests for project status transitions
- unit tests for project cost rollups
- unit tests for reversal and return flows
- integration tests for purchase-order -> receive -> material issue -> cost summary
- controller tests for validation and error payload consistency

## Recommendation

Proceed to the project module only after Phase 1 hardening is done. The project foundation is already in place, but inventory/project state integrity should be tightened first so later project features are built on reliable stock behavior.
