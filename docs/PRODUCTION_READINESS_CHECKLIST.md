# Production Readiness Checklist

## Purpose

This checklist helps decide whether the current `Inventory` and `Project` modules are ready for live operational use for a construction client.

It is based on the current codebase state as of May 26, 2026.

## Overall Assessment

Current practical status:

- `Inventory module`: solid for core operations, but not yet fully mature
- `Project module`: solid for core operations, but not yet fully mature

Current go-live opinion:

- both modules are strong enough for a controlled first rollout
- both modules still need a final hardening pass before they should be treated as fully mature ERP modules

## Inventory Module

### What Is Already Strong

- product, category, supplier, warehouse, unit, and customer master data
- purchase order create, approve, receive, and cancel
- purchase return
- sales order create, confirm, ship, and cancel
- sales return
- warehouse stock inquiry
- reserved stock behavior for confirmed sales orders
- stock adjustment
- stock transfer
- stock movement history
- project material issue
- project material return
- project material issue reversal
- stronger validation around warehouse and `performedBy`

### What Still Needs Hardening

- receiving flow is still simple compared with real warehouse edge cases
- movement reporting is still missing richer filters such as date-driven operational reporting
- audit/report depth can still improve for supervisors and management

### Production Checklist

- confirm warehouse users understand the difference between:
  - stock adjustment
  - transfer
  - project issue
  - return
  - reversal
- decide whether partial receiving is required by the real client process
- verify that all stock-affecting endpoints are covered by role permissions
- test month-end stock reconciliation using real sample data
- test error paths for wrong warehouse, wrong quantity, and duplicate posting attempts

### Inventory Go-Live Rating

- current readiness: `strong core / not fully mature`
- estimated completion: `about 94%`

## Project Module

### What Is Already Strong

- project create, update, list, and fetch
- controlled lifecycle transitions
- project-linked purchase orders
- project material issue integration
- project material return integration
- project issue reversal support
- simple project budget amount
- project budget lines
- committed vs actual cost summary
- return-aware cost summary
- project movement ledger
- project dashboard endpoint

### What Still Needs Hardening

- no reopen workflow if the business later wants controlled reopening
- budget lines are present, but not yet grouped into richer cost structures such as phases or cost categories
- management reporting is still functional, not yet executive-level
- no approval layer for project decisions
- no deeper audit trail for major project control actions

### Production Checklist

- confirm the client’s project status policy matches the current transition rules
- decide whether reopen from `COMPLETED` or `CANCELLED` is needed
- define the budget structure the client actually wants:
  - simple line items only
  - cost categories
  - project phase budgets
- verify that committed cost should remain PO-based for the client
- validate dashboard numbers against sample live-style projects
- test project closure scenarios after issues, returns, and reversals
- verify role permissions for:
  - project editing
  - budget-line maintenance
  - project completion
  - cancellation

### Project Go-Live Rating

- current readiness: `strong core / not fully mature`
- estimated completion: `about 91%`

## Recommended Before Live Use

These are the highest-value next steps before calling both modules fully solid:

1. validate role/permission coverage for all stock and project actions
2. run end-to-end UAT using one full construction scenario from PO to project close
3. prepare operating SOPs for warehouse issue, return, reversal, and project closure
4. decide whether approval or reason-code rules are required around purchase and sales returns
5. deepen reporting and reconciliation views for supervisors and month-end checks

## Final Recommendation

If the goal is:

- `controlled pilot or first client rollout`: the modules are already usable
- `fully mature production confidence`: do one more hardening cycle first

In short:

- the modules are no longer fragile
- the main remaining work is correction completeness, reporting depth, and operational governance
