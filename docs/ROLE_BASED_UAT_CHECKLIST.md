# Role-Based UAT Checklist

## Purpose

This checklist is for validating the current `Inventory`, `Project`, and `Identity` behavior using real user roles before pilot rollout or production use.

It is designed around the current system state as of June 2, 2026.

## Roles To Prepare

Create or assign at least one test user for each of these roles:

- `ADMIN`
- `PROJECT_MANAGER`
- `PROCUREMENT`
- `WAREHOUSE`
- `VIEWER`

Use realistic names and email addresses so audit fields like `performedBy` are easy to validate.

## Test Data To Prepare

Before testing, prepare:

- at least 1 active customer
- at least 1 active supplier
- at least 3 active products
- at least 1 active unit
- at least 1 active warehouse
- at least 1 test project with budget

Recommended project scenario:

- one construction project
- one project-linked purchase order
- one project material issue
- one project material return

## Global Checks

Run these checks for every role:

1. Confirm login succeeds and session persists correctly.
2. Confirm sidebar only shows the pages appropriate for the role.
3. Confirm read-only users do not see write buttons for restricted pages.
4. Confirm restricted API actions fail cleanly if triggered outside allowed roles.
5. Confirm all success and error messages are understandable.

## Admin UAT

Expected access:

- full system access
- user and role management
- full inventory access
- full project access

Checklist:

1. Log in as `ADMIN`.
2. Open `/users`.
3. Create a new user.
4. Assign that user one of each business role.
5. Confirm `Users` page and role drawer work correctly.
6. Create a project from `/projects`.
7. Edit the project and update budget details.
8. Add budget lines.
9. Create a project-linked purchase order from `/projects/[id]`.
10. Approve and receive the PO from the project workspace.
11. Create a material issue from the project workspace.
12. Create a material return from the project workspace.
13. Reverse a material issue from the project workspace.
14. Create a purchase return from the project workspace.
15. Confirm dashboard, movement ledger, and cost summary update correctly.

Pass criteria:

- all module pages visible
- no permission denials for valid actions
- budget, movement, and return/reversal data refresh correctly

## Project Manager UAT

Expected access:

- project create and edit
- project budget maintenance
- project lifecycle actions
- project reporting visibility
- no user admin access

Checklist:

1. Log in as `PROJECT_MANAGER`.
2. Confirm `/users` is hidden or blocked.
3. Open `/projects`.
4. Create a project.
5. Edit the project.
6. Add budget lines.
7. Activate the project.
8. Move the project to `ON_HOLD`.
9. Resume the project.
10. Review dashboard, cost summary, and movement tabs.
11. Confirm procurement and warehouse actions are only visible if intentionally allowed by your current UI rules.

Pass criteria:

- project management works
- user admin is not accessible
- project data is visible and correct

## Procurement UAT

Expected access:

- master data writes for procurement-owned pages
- purchase order flow access
- supplier/customer/category/product/unit management
- no project management access
- no stock adjustment/transfer access

Checklist:

1. Log in as `PROCUREMENT`.
2. Confirm `/projects` is visible only in read-only mode or limited mode according to your current rules.
3. Open `/suppliers`, `/customers`, `/categories`, `/products`, `/units`.
4. Confirm create/edit actions are available.
5. Open `/purchase-orders`.
6. Create a purchase order.
7. Approve the purchase order.
8. Receive the purchase order if your current policy allows procurement-side receive visibility.
9. Create a purchase return.
10. Open a project workspace and confirm project-linked PO create/approve/receive/return visibility matches your intended procurement role behavior.

Pass criteria:

- procurement pages allow writes
- unrelated admin pages are blocked
- procurement actions do not expose warehouse-only stock tools unless intentionally allowed

## Warehouse UAT

Expected access:

- stock inquiry
- stock adjustment
- stock transfer
- sales order flow
- material issue, return, reversal
- no supplier/category/product admin unless intentionally allowed

Checklist:

1. Log in as `WAREHOUSE`.
2. Open `/stocks`.
3. Confirm stock filters work.
4. Perform a stock adjustment.
5. Perform a stock transfer.
6. Open `/sales-orders`.
7. Create a sales order.
8. Confirm the sales order.
9. Ship the sales order.
10. Create a sales return.
11. Open a project workspace.
12. Create a material issue.
13. Create a material return.
14. Reverse a material issue.
15. Confirm movement ledger reflects the operations.

Pass criteria:

- warehouse actions succeed
- procurement-only pages are read-only or blocked
- project site-usage corrections work

## Viewer UAT

Expected access:

- read-only access only
- no user admin
- no write buttons on inventory/project pages

Checklist:

1. Log in as `VIEWER`.
2. Open `/projects`.
3. Open `/projects/[id]`.
4. Open `/purchase-orders`, `/sales-orders`, `/stocks`, `/products`, `/suppliers`, `/customers`.
5. Confirm data is visible.
6. Confirm create/edit/approve/receive/ship/return/reversal buttons are hidden.
7. Confirm `/users` is not visible in the sidebar and cannot be used.

Pass criteria:

- read access works
- write controls are not shown
- direct restricted routes fail safely if visited manually

## End-to-End Scenario

Use one final end-to-end scenario after role testing:

1. `ADMIN` or `PROJECT_MANAGER` creates a project.
2. `PROCUREMENT` creates a project-linked PO.
3. `PROCUREMENT` approves the PO.
4. `PROCUREMENT` or allowed role receives the PO into a warehouse.
5. `WAREHOUSE` creates a project material issue.
6. `WAREHOUSE` creates a partial material return.
7. `WAREHOUSE` reverses a remaining issue balance.
8. `PROCUREMENT` creates a purchase return if needed.
9. Review project dashboard, budget variance, and movements.

Pass criteria:

- all quantities and values remain consistent
- returns and reversals appear correctly
- project dashboard and cost summary reflect net usage

## Defect Logging Template

For each failed test, record:

- role used
- page or route
- action attempted
- expected result
- actual result
- screenshot or payload
- severity

## Exit Criteria

You can treat this UAT cycle as passed when:

1. each role completes its checklist without critical blockers
2. no restricted role can perform blocked actions
3. no allowed role is prevented from completing its core workflow
4. project and inventory totals remain consistent after corrections
5. all known defects are either fixed or explicitly accepted
