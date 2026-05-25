# Inventory Module Flow

This document describes the current implemented flow for the inventory and project-linked material operations as of May 25, 2026.

## Overview

The module currently supports:

- master data setup for categories, suppliers, warehouses, units, and customers
- project setup, update, status changes, and project cost summary lookup
- update and deactivate flows for categories, suppliers, warehouses, and products
- product creation, update, deactivate, and lookup
- purchase order creation, approval, receiving, cancellation, project assignment, and lookup
- sales order creation, confirmation, shipping, cancellation, and lookup
- project material issue creation and lookup
- stock lookup by product and warehouse
- stock movement history lookup
- manual stock adjustment
- warehouse-to-warehouse stock transfer

Stock is tracked per:

- `productId`
- `warehouseId`

Each stock row stores:

- `quantityOnHand`
- `reservedQuantity`
- `reorderLevel`
- `updatedAt`

Available stock is:

```text
quantityOnHand - reservedQuantity
```

## Main Process

The normal operating flow is:

1. Create categories, suppliers, warehouses, units, customers, and projects.
2. Create products.
3. Create a purchase order, optionally linked to a project.
4. Approve the purchase order.
5. Receive the purchase order into a warehouse.
6. Stock becomes available in that warehouse.
7. Create a sales order, or issue materials directly to a project.
8. Confirm the sales order against a warehouse when outbound stock is for customer shipping.
9. Stock becomes reserved for sales orders only.
10. Ship the sales order from that warehouse, or post a material issue to reduce stock for a project.
11. Stock movements remain available for audit and costing.

Operational correction flows are also supported:

- cancel a purchase order before it is received
- cancel a sales order before it is shipped or delivered
- adjust stock up or down
- transfer stock between warehouses
- review the movement ledger for audit/history

## Project Flow

Projects can be:

- created
- updated
- listed
- fetched by ID
- moved to `ACTIVE`, `ON_HOLD`, `COMPLETED`, or `CANCELLED`

Current behavior:

- project code is required and must be unique
- project name is required
- customer must exist
- project starts in `DRAFT`
- completed or cancelled projects cannot change status again

## Master Data Flow

### Categories

Categories can be:

- created
- updated
- deactivated
- listed
- fetched by ID

Current behavior:

- category name is required
- parent category is optional
- if `parentId` is provided, the parent category must exist
- a category cannot be its own parent
- a category cannot be deactivated while products still reference it

### Suppliers

Suppliers can be:

- created
- updated
- deactivated
- listed
- fetched by ID

Current behavior:

- supplier code is required
- supplier name is required
- supplier code must be unique
- a supplier cannot be deactivated while non-cancelled purchase orders still reference it

### Warehouses

Warehouses can be:

- created
- updated
- deactivated
- listed
- fetched by ID

Current behavior:

- warehouse code is required
- warehouse name is required
- warehouse code must be unique
- a warehouse cannot be deactivated while on-hand or reserved stock still exists there

### Units

Units can be:

- created
- listed
- fetched by ID

Current behavior:

- unit name is required
- unit name must be unique

### Customers

Customers can be:

- created
- listed
- fetched by ID

Current behavior:

- customer code is required
- customer name is required
- customer code must be unique

## Product Flow

Products can be:

- created
- updated
- deactivated
- listed
- fetched by ID

Current behavior:

- SKU must be unique
- product is created as active
- if `categoryId` is provided, the category must exist
- if `unitId` is provided, the unit must exist
- a product cannot be deactivated while on-hand or reserved stock still exists

Important notes:

- creating a product does not create stock
- stock rows are created when inventory is first received, adjusted, or transferred in

## Purchase Order Flow

### 1. Create Purchase Order

When a purchase order is created:

- supplier must exist
- if `projectId` is provided, the project must exist
- order date is required
- at least one item is required
- each item must have a valid product
- each item quantity must be greater than zero
- each item unit cost must be zero or greater
- total amount is calculated from all item line totals
- status starts as `DRAFT`
- a `poNumber` is generated automatically

### 2. Approve Purchase Order

Approval rules:

- only `DRAFT` purchase orders can be approved
- purchase order must contain items

After approval:

- status becomes `APPROVED`

### 3. Receive Purchase Order

Receiving rules:

- only `APPROVED` purchase orders can be received
- receiving requires a target warehouse
- receiving requires `performedBy`

On receive:

- each item increases warehouse stock
- stock movement entries are created with movement type `PURCHASE_IN`
- purchase order status becomes `RECEIVED`

### 4. Cancel Purchase Order

Cancellation rules:

- `DRAFT` and `APPROVED` purchase orders can be cancelled
- `RECEIVED` purchase orders cannot be cancelled

On cancel:

- status becomes `CANCELLED`

## Sales Order Flow

### 1. Create Sales Order

When a sales order is created:

- customer ID is required
- customer must exist
- order date is required
- at least one item is required
- each item product must exist
- each item quantity must be greater than zero
- each item unit price must be zero or greater
- total amount is calculated from all item line totals
- status starts as `DRAFT`
- an `soNumber` is generated automatically

### 2. Confirm Sales Order

Confirmation rules:

- only `DRAFT` sales orders can be confirmed
- warehouse must exist
- sales order must contain items
- each product must be active
- each item must have enough available stock in the selected warehouse

On confirm:

- stock is reserved
- status becomes `CONFIRMED`

### 3. Ship Sales Order

Shipping rules:

- only `CONFIRMED` sales orders can be shipped
- warehouse must exist
- shipping requires `performedBy`

On ship:

- reserved stock is consumed
- `quantityOnHand` is reduced
- `reservedQuantity` is reduced
- stock movement entries are created with movement type `SALES_OUT`
- sales order status becomes `SHIPPED`

### 4. Cancel Sales Order

Cancellation rules:

- `DRAFT` sales orders can be cancelled directly
- `CONFIRMED` sales orders can be cancelled if a warehouse is provided so reserved stock can be released
- `SHIPPED` and `DELIVERED` sales orders cannot be cancelled

On cancel:

- status becomes `CANCELLED`
- if the order was `CONFIRMED`, reserved stock is released

## Material Issue Flow

### 1. Create Material Issue

When a material issue is created:

- project must exist
- project must be `ACTIVE`
- warehouse must exist
- `performedBy` is required
- at least one item is required
- each item must have a valid active product
- each item quantity must be greater than zero
- each item unit cost must be zero or greater
- each item must have enough available stock in the selected warehouse

On issue:

- `quantityOnHand` is reduced
- `reservedQuantity` is unchanged
- a material issue header and line items are stored
- stock movement entries are created with movement type `PROJECT_ISSUE`
- each movement is tagged with `projectId`

## Project Cost Summary

Supported lookup:

- `GET /projects/{id}/cost-summary`

Current summary includes:

- total purchase order amount linked to the project
- total material issued cost linked to the project
- material issue totals grouped by product

## Stock Operations

### Stock Inquiry

Supported lookups:

- by product
- by warehouse
- by product and warehouse

### Stock Movements

Movement history can be fetched by:

- `productId`
- `warehouseId`
- `referenceId`

Movement rows currently include:

- movement type
- quantity
- unit cost
- project ID when the movement is project-related
- reference type
- reference ID
- remarks
- performed by
- movement date

### Stock Adjustment

Supported adjustment types:

- `INCREASE`
- `DECREASE`

Rules:

- product must exist and be active
- warehouse must exist
- quantity must be greater than zero
- decrease cannot consume more than available stock

On adjustment:

- `quantityOnHand` changes
- `reservedQuantity` is unchanged
- a stock movement is created with movement type `ADJUSTMENT`

### Stock Transfer

Transfer rules:

- product must exist and be active
- source warehouse must exist
- destination warehouse must exist
- source and destination warehouses must be different
- quantity must be greater than zero
- source warehouse must have enough available stock

On transfer:

- source `quantityOnHand` decreases
- destination `quantityOnHand` increases
- two stock movements are created with movement type `TRANSFER`
- both movement rows share one generated transfer reference ID

## Stock Rules

Current stock behavior:

- one stock row is enforced per `productId + warehouseId`
- receiving inventory increases `quantityOnHand`
- confirming a sales order increases `reservedQuantity`
- shipping a sales order decreases both `quantityOnHand` and reserved stock
- project material issues decrease only `quantityOnHand`
- cancelling a confirmed sales order decreases `reservedQuantity`
- adjustments only change `quantityOnHand`
- transfers change `quantityOnHand` in both warehouses

This means:

- received stock becomes available to sell or issue
- confirmed stock is held for shipping
- shipped stock leaves the warehouse
- project issues consume available stock for a construction job
- cancelled confirmed orders release reservations back to available stock

## Exception Handling

Inventory and project services use typed business exceptions instead of relying on generic `RuntimeException` for normal business failures.

Current exception categories:

- `BadRequestException`
- `ConflictException`
- `NotFoundException`

The global exception handler maps these to HTTP responses with a consistent payload:

```json
{
  "code": "BAD_REQUEST",
  "error": "Human readable message"
}
```

## Current API Surface

### Projects

- `POST /projects`
- `PUT /projects/{id}`
- `GET /projects`
- `GET /projects/{id}`
- `POST /projects/{id}/activate`
- `POST /projects/{id}/hold`
- `POST /projects/{id}/complete`
- `POST /projects/{id}/cancel`
- `GET /projects/{id}/cost-summary`

### Master Data

- `POST /inventory/categories`
- `PUT /inventory/categories/{id}`
- `POST /inventory/categories/{id}/deactivate`
- `GET /inventory/categories`
- `GET /inventory/categories/{id}`
- `POST /inventory/suppliers`
- `PUT /inventory/suppliers/{id}`
- `POST /inventory/suppliers/{id}/deactivate`
- `GET /inventory/suppliers`
- `GET /inventory/suppliers/{id}`
- `POST /inventory/warehouses`
- `PUT /inventory/warehouses/{id}`
- `POST /inventory/warehouses/{id}/deactivate`
- `GET /inventory/warehouses`
- `GET /inventory/warehouses/{id}`
- `POST /inventory/units`
- `GET /inventory/units`
- `GET /inventory/units/{id}`
- `POST /inventory/customers`
- `GET /inventory/customers`
- `GET /inventory/customers/{id}`

### Products

- `POST /inventory/products`
- `PUT /inventory/products/{id}`
- `POST /inventory/products/{id}/deactivate`
- `GET /inventory/products`
- `GET /inventory/products/{id}`

### Purchase Orders

- `POST /inventory/purchase-orders`
- `GET /inventory/purchase-orders`
- `GET /inventory/purchase-orders/{id}`
- `POST /inventory/purchase-orders/{id}/approve`
- `POST /inventory/purchase-orders/{id}/receive`
- `POST /inventory/purchase-orders/{id}/cancel`

### Sales Orders

- `POST /inventory/sales-orders`
- `GET /inventory/sales-orders`
- `GET /inventory/sales-orders/{id}`
- `POST /inventory/sales-orders/{id}/confirm`
- `POST /inventory/sales-orders/{id}/ship`
- `POST /inventory/sales-orders/{id}/cancel`

### Material Issues

- `POST /inventory/material-issues`
- `GET /inventory/material-issues`
- `GET /inventory/material-issues/{id}`

### Inventory

- `GET /inventory/stocks?productId=...`
- `GET /inventory/stocks?warehouseId=...`
- `GET /inventory/stocks?productId=...&warehouseId=...`
- `GET /inventory/stock-movements?productId=...`
- `GET /inventory/stock-movements?warehouseId=...`
- `GET /inventory/stock-movements?referenceId=...`
- `POST /inventory/stocks/adjustments`
- `POST /inventory/stocks/transfers`

## Current Limitations

The module is in a stronger construction-ready state now, but these gaps still remain:

- no return flow yet for purchase, sales, or project issue operations
- no reverse or reopen flow for received purchase orders, shipped sales orders, or posted material issues
- no explicit reactivation endpoints for categories, suppliers, warehouses, products, or projects
- no project budget and variance layer yet, only summary costing
- test coverage is improving but still lighter than the total business surface area

## Suggested Next Steps

The most practical next improvements are:

1. Add return flows for inbound, outbound, and project issue inventory.
2. Add project material returns or reversal flows for incorrect issues.
3. Expand automated tests around project status transitions and project-linked procurement.
4. Add richer stock movement filters such as date range, movement type, and project ID.
5. Introduce project budgets and variance reporting if the business needs deeper costing.
