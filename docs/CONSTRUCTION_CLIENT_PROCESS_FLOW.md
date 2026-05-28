# Construction Client Process Flow

## Purpose

This document translates the current ERP system into a construction-specific operating flow for a client that runs projects, procures materials, stores inventory in warehouses, and issues materials to job sites.

It reflects the current backend behavior as of May 26, 2026, including project budgets, project material returns, material issue reversal, project movement lookup, and return-aware project cost summaries.

Monetary values referenced in this document should be treated as Philippine peso (PHP).

## Business Context

The client workflow assumed here is:

- the company runs multiple construction projects at the same time
- each project belongs to a customer or contract owner
- materials are purchased from suppliers
- materials are received into a warehouse or stock point
- materials are issued from warehouse stock to a project when needed on site
- unused or excess issued materials may be returned from site
- some items may also be sold or delivered outward through sales orders
- management needs project-level visibility into procurement, issued cost, returned cost, and net material consumption

## Core Roles

Typical users in the client organization:

- `Admin`: maintains setup and user access
- `Procurement Officer`: creates and manages purchase orders
- `Warehouse Staff`: receives, adjusts, transfers, issues, and accepts material returns
- `Project Engineer / Site Engineer`: requests materials for a project
- `Project Manager`: monitors project status, budget, and cost usage
- `Finance / Management`: reviews procurement totals and project cost summaries

## System Flow Summary

The current system fits this real-world construction flow:

1. Set up master data.
2. Create a construction project.
3. Encode materials and units of measure.
4. Purchase materials for general stock or for a specific project.
5. Receive purchased materials into a warehouse.
6. Monitor available stock by warehouse.
7. Issue materials from warehouse stock to an active project.
8. Return unused materials from site when necessary.
9. Reverse wrongly posted material issues when needed.
10. Track stock movement and project-linked consumption.
11. Review project cost summary using procurement, issue, return, and net material totals.

## Detailed Operating Flow

### 1. Initial Setup

Before live operations begin, the company prepares:

- customers
- suppliers
- warehouses
- units
- categories
- products

Construction examples:

- categories: `Cement`, `Steel`, `Electrical`, `Plumbing`, `Finishing`
- units: `bag`, `pc`, `meter`, `kg`, `roll`
- warehouses: `Main Warehouse`, `Cebu Yard`, `Site Buffer Warehouse`
- products: `40kg Portland Cement`, `10mm Rebar`, `PVC Pipe 1/2`, `THHN Wire 2.0mm`

Purpose:

- keep procurement, stock, and project costing standardized
- avoid duplicate material names and inconsistent units

### 2. Project Setup

For each construction job, the team creates a project record.

Typical project data:

- project code
- project name
- customer
- location
- planned start date
- target end date
- optional project budget amount

Current system behavior:

- new projects start in `DRAFT`
- once approved internally, the project can be moved to `ACTIVE`
- active projects can receive material issues
- projects may optionally carry a simple budget amount for variance tracking
- completed or cancelled projects are closed for further status changes

Construction meaning:

- `DRAFT`: project record is encoded but not yet operational
- `ACTIVE`: site is ongoing and may consume materials
- `ON_HOLD`: site work is temporarily paused
- `COMPLETED`: work is done
- `CANCELLED`: project was stopped or terminated

### 3. Procurement Flow

The procurement officer creates a purchase order when materials are needed.

The PO may be:

- for common warehouse replenishment
- linked to a specific project for cost visibility

Construction examples:

- buy 500 bags of cement for general stock
- buy steel bars tagged to `Project PRJ-001`
- buy electrical materials intended for a high-rise fit-out project

Current supported flow:

1. Create purchase order with supplier, date, and line items.
2. Optionally tag the purchase order to a project.
3. Approve the purchase order.
4. Receive the purchase order into a warehouse.

What this means operationally:

- procurement records the commercial intent to buy
- warehouse receiving is the point when stock becomes available in the system
- project-linked PO amounts can later appear in the project cost summary

### 4. Warehouse Receiving Flow

When delivered materials arrive:

- warehouse staff checks the supplier delivery
- confirms the warehouse destination
- receives the approved purchase order

System effect:

- stock increases in the selected warehouse
- a `PURCHASE_IN` movement is recorded
- the purchase order becomes `RECEIVED`

Construction meaning:

- materials are now physically and systemically available for site issuance, transfer, or future sale

### 5. Stock Monitoring Flow

Warehouse and project teams can review stock by:

- product
- warehouse
- product plus warehouse

This helps answer questions like:

- how many cement bags are still available in the main warehouse
- which warehouse holds the remaining electrical stock
- whether a project request can be fulfilled immediately

Important stock logic:

- `quantityOnHand` is physical stock recorded
- `reservedQuantity` is stock held for confirmed sales orders
- available stock is `quantityOnHand - reservedQuantity`

For construction use:

- only available stock should be considered ready for project issue

### 6. Material Request to Project Flow

This is the most important construction-specific process.

Real-world operating pattern:

1. Site engineer identifies required materials for ongoing work.
2. Warehouse checks availability.
3. Warehouse issues materials against the active project.
4. The issue is recorded with quantities, cost, warehouse, and performer.

Current system support:

- a material issue can be posted only to an `ACTIVE` project
- the source warehouse must exist
- each item must be an active product
- each item must have enough available stock

System effect:

- warehouse stock decreases
- a `PROJECT_ISSUE` movement is created
- the movement is tagged with `projectId`
- a material issue header and line items are saved

Construction meaning:

- this acts like a warehouse release slip to site
- issued materials become part of project consumption
- project cost reporting can reflect actual material usage

Examples:

- issue 100 bags of cement to foundation works
- issue 50 steel bars to slab reinforcement
- issue 20 rolls of wire to electrical rough-in

### 6A. Material Return From Project Flow

Sometimes the site sends materials back because of:

- excess release
- unused materials after a task is done
- wrong item picked from warehouse
- partial work cancellation

Current system support:

- return is posted against an existing material issue
- stock goes back to the same warehouse used by the original issue
- returned quantity cannot exceed the originally issued quantity for a product

System effect:

- warehouse stock increases
- a `RETURN` movement is created
- a separate material return record is stored for audit

Construction meaning:

- this acts like a warehouse return slip from site
- project net consumption is reduced when materials come back

### 6B. Material Issue Reversal Flow

Sometimes a warehouse posting is simply wrong and needs a formal reversal, not just a normal return.

Examples:

- wrong quantity was issued to the project
- wrong product was posted out
- the issue was posted before actual release happened

Current system support:

- reversal is posted against an existing material issue
- only the remaining unrecovered quantity can be reversed
- stock goes back to the same warehouse used by the original issue
- the reversal is stored as a return record with a `reversal` flag for audit clarity

System effect:

- warehouse stock increases
- a `RETURN` movement is created
- the correction stays visible in the audit trail

Construction meaning:

- the team can correct a bad issue without deleting history
- prior manual returns are respected, so only the remaining quantity is reversed

### 7. Inter-Warehouse Transfer Flow

Construction companies often move stock between stock points.

Examples:

- transfer materials from main warehouse to provincial yard
- move stock from central warehouse to a temporary site warehouse

Current system support:

- transfer stock from one warehouse to another
- system prevents transfer when available stock is insufficient
- system records paired `TRANSFER` movements

Construction meaning:

- stock can be repositioned closer to the site before actual issue
- logistics movement remains auditable

Recommended usage:

- use transfer when stock is still company-owned and just relocating
- use material issue when stock is being consumed by a project

### 8. Stock Adjustment Flow

Warehouse staff may need to correct stock due to:

- counting discrepancies
- damaged materials
- spoilage
- unrecorded receipts
- breakage or wastage found during audit

Current system support:

- increase adjustment
- decrease adjustment

Construction meaning:

- this is for inventory correction, not normal project consumption
- project usage should go through material issue or material return, not stock adjustment

Recommended business rule:

- require a clear reason for every adjustment for audit discipline

### 9. Outbound Sales Flow

The system also supports sales orders.

For a construction client, this may apply when:

- excess materials are sold
- materials are released to another party through a formal sales flow
- the client operates both contracting and trading activities

Current system support:

1. Create sales order.
2. Confirm it against a warehouse.
3. Stock becomes reserved.
4. Ship the order.
5. Return sold materials back into the original sales warehouse when needed.

Construction interpretation:

- use this only for commercial outbound deliveries
- do not use sales orders for normal internal project issuance
- sales returns now support customer-side correction without relying on stock adjustment

### 10. Project Cost Monitoring Flow

Management can review project cost summary using:

- project budget amount when set
- project-linked purchase order totals
- material issue totals grouped by product
- material return totals grouped by product
- net material issued totals after returns

Project teams can also review a project movement ledger using:

- `GET /projects/{id}/movements`

Management and project leads can also review a project dashboard using:

- `GET /projects/{id}/dashboard`

This ledger shows:

- project-linked issues
- returns
- reversal-tagged returns
- newest activity first

The dashboard is designed for fast monitoring and includes:

- budget basis
- committed cost
- actual net material cost
- issue and return quantities
- reversal count
- last project movement activity

This helps answer:

- how much has been procured for the project
- how much material cost has already been issued to site
- how much material cost has been returned from site
- what the net material consumption really is
- which materials are driving current consumption

Current state:

- project budgeting now supports project-level budget lines for a more structured material budget basis
- the cost summary now distinguishes committed cost and actual net material cost
- the current view is still a useful operational cost summary, not yet full project controls

### 11. Project Closure Flow

When a construction project is done:

- the project can be marked `COMPLETED`

If the project stops permanently:

- it can be marked `CANCELLED`

Before closure, the business should ideally check:

- pending material issues are complete
- material returns are settled
- major warehouse corrections are settled
- project cost summary is reviewed

Current note:

- returns and issue reversal are now supported for project material corrections

## Recommended Client Operating Policy

To keep the construction client's operations clean, the best practical usage is:

- use purchase orders for buying materials
- use warehouse receiving when materials physically arrive
- use transfers when materials are only being moved between company stock points
- use material issues when materials are consumed by a project
- use material returns when project-issued materials come back from site
- use sales orders only for customer-facing outward delivery, not internal site requests
- use stock adjustments only for corrections, not as a substitute for project issue

## Example End-to-End Scenario

Example:

1. Create project `PRJ-CEBU-001 - Three Storey Residence`.
2. Activate the project.
3. Create products such as cement, rebar, wire, and PVC pipe.
4. Create a purchase order for cement and rebar, tagged to the project.
5. Approve the purchase order.
6. Receive the materials into `Main Warehouse`.
7. Transfer part of the stock to `Site Buffer Warehouse` if needed.
8. Post material issue from the warehouse to the active project as work begins.
9. If excess materials come back from site, post a material return against the original issue.
10. Review stock movements to confirm what was received, transferred, issued, and returned.
11. Check the project cost summary to see procurement amount, return amount, and net material cost.
12. Mark the project completed when construction is finished.

## Current Gaps The Client Should Know

The process is already usable for core construction material control, but these items are still future improvements:

- project budgeting is present, but not yet grouped into richer cost structures such as phases or formal cost categories
- project workflow testing is still lighter than the inventory test surface

## Best Next Enhancement For This Client

The strongest next improvements for a construction client are:

1. add broader correction workflows for purchasing and outbound sales
2. expand project budgets into richer grouped budget control
3. add project movement and consumption dashboards
4. add approval and governance rules around corrections

## Conclusion

Your current ERP already matches the backbone of a construction materials control process:

- project setup
- procurement
- receiving
- warehouse tracking
- site issuance
- site return handling
- project-linked cost visibility

What remains is mostly deeper controls and richer correction workflows, not rebuilding the process from scratch.
