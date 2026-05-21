# Inventory Module Flow

This document describes the current business flow implemented in the inventory module.

## Overview

The module currently supports:

- master data setup for categories, suppliers, and warehouses
- product creation and lookup
- purchase order creation, approval, receiving, and lookup
- sales order creation, confirmation, shipping, and lookup
- stock lookup by product and warehouse

Stock is tracked per:

- `productId`
- `warehouseId`

Each stock record currently stores:

- `quantityOnHand`
- `reservedQuantity`
- `reorderLevel`
- `updatedAt`

Available stock is:

```text
quantityOnHand - reservedQuantity
```

## Main Process

The normal operational flow is:

1. Create category, supplier, and warehouse.
2. Create product.
3. Create purchase order.
4. Approve purchase order.
5. Receive purchase order into a warehouse.
6. Stock becomes available in that warehouse.
7. Create sales order.
8. Confirm sales order against a warehouse.
9. Stock becomes reserved.
10. Ship sales order from that warehouse.
11. Reserved stock is consumed and on-hand stock is reduced.

## Master Data Flow

### Categories

Categories can be:

- created
- listed
- fetched by ID

Current behavior:

- category name is required
- parent category is optional
- if `parentId` is provided, the parent category must exist

### Suppliers

Suppliers can be:

- created
- listed
- fetched by ID

Current behavior:

- supplier code is required
- supplier name is required
- supplier code must be unique

### Warehouses

Warehouses can be:

- created
- listed
- fetched by ID

Current behavior:

- warehouse code is required
- warehouse name is required
- warehouse code must be unique

## Product Flow

Products can be:

- created
- listed
- fetched by ID

Current behavior:

- SKU must be unique
- product is created as active
- if `categoryId` is provided, the category must exist
- `unitId` is stored but is not yet backed by a real unit module or validation

Important note:

- creating a product does not create stock
- stock only appears when inventory is received through purchase orders

## Purchase Order Flow

### 1. Create Purchase Order

When a purchase order is created:

- supplier must exist
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

## Sales Order Flow

### 1. Create Sales Order

When a sales order is created:

- customer ID is required
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

## Stock Rules

Current stock behavior:

- one stock row is enforced per `productId + warehouseId`
- receiving inventory increases `quantityOnHand`
- confirming a sales order increases `reservedQuantity`
- shipping a sales order decreases both `quantityOnHand` and reserved stock

This means:

- received stock becomes available to sell
- confirmed stock is held for shipping
- shipped stock leaves the warehouse

## Current API Surface

### Master Data

- `POST /inventory/categories`
- `GET /inventory/categories`
- `GET /inventory/categories/{id}`
- `POST /inventory/suppliers`
- `GET /inventory/suppliers`
- `GET /inventory/suppliers/{id}`
- `POST /inventory/warehouses`
- `GET /inventory/warehouses`
- `GET /inventory/warehouses/{id}`

### Products

- `POST /inventory/products`
- `GET /inventory/products`
- `GET /inventory/products/{id}`

### Purchase Orders

- `POST /inventory/purchase-orders`
- `GET /inventory/purchase-orders`
- `GET /inventory/purchase-orders/{id}`
- `POST /inventory/purchase-orders/{id}/approve`
- `POST /inventory/purchase-orders/{id}/receive`

### Sales Orders

- `POST /inventory/sales-orders`
- `GET /inventory/sales-orders`
- `GET /inventory/sales-orders/{id}`
- `POST /inventory/sales-orders/{id}/confirm`
- `POST /inventory/sales-orders/{id}/ship`

### Inventory Stock

- `GET /inventory/stocks?productId=...`
- `GET /inventory/stocks?warehouseId=...`
- `GET /inventory/stocks?productId=...&warehouseId=...`

## Current Limitations

The module is working, but these areas are still not finished:

- `unitId` is stored on products but not validated against a real unit master
- customer master data is not yet implemented
- supplier, warehouse, and category are currently create/read only
- no purchase order cancel flow
- no sales order cancel flow
- no stock adjustment flow
- no stock transfer flow
- no return flow
- exceptions are still generic `RuntimeException`
- HTTP/controller coverage exists but is still light

## Suggested Next Steps

If the module will keep growing, the next practical improvements are:

1. Add update/deactivate endpoints for category, supplier, and warehouse.
2. Add a unit master module and validate `unitId`.
3. Add customer validation for sales orders.
4. Introduce typed business exceptions instead of generic runtime exceptions.
5. Add stock adjustment, transfer, and return flows.
