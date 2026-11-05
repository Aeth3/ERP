# AP Bills & Payments (Roadmap Step 3) — Design

Date: 2026-11-05
Status: Approved design, pending implementation plan

## Goal

Add accounts payable to the ERP: supplier bills (PO-backed with 3-way match, and
non-PO expense bills) and payments against them, fully posted to the general
ledger built in step 2. Success: a received PO can be billed and paid with
correct GRNI clearing, AP balance, and aging visibility; operating expenses can
be recorded as non-PO bills.

## Decisions (confirmed 2026-11-05)

- Keep whole-PO receiving. The "three ways" are: PO (order), PO in `RECEIVED`
  status (receipt), bill (invoice). Partial receipts/GRN are a later step.
- Price variance vs the PO never blocks a bill; it posts to a Purchase Price
  Variance account. Over-billing quantity is a hard block.
- Non-PO bills are in scope: lines coded directly to GL expense accounts.
- Simple payments only: full or partial, allocated across one or more bills.
  No payment runs, batching, or check printing.
- Amounts are VAT-inclusive (standing decision until roadmap step 5).
- Money is PHP; business time zone Asia/Manila; calendar fiscal year.

## Placement

AP lives inside `modules/finance` (bills and payments are finance documents).
GL posting is a direct in-module call to `LedgerPostingService`. Finance gets
PO data through a new port it defines, `PurchaseOrderReader`, implemented by an
adapter in the inventory module — the mirror image of the existing
`InventoryLedgerPoster` pattern (port in inventory, implementation in finance).

## Data model

All new tables via an idempotent migration (`V20261105_01__accounts_payable.sql`)
**and** matching JPA entities, since Flyway does not currently run and schema
comes from Hibernate `ddl-auto=update`. Follow the existing
domain model / entity / repository-interface / impl / jpa layering of the
finance module.

### Bill (`ap_bills`)
- `id` UUID, `billNumber` (supplier's invoice number; unique per supplier),
  `supplierId`, `purchaseOrderId` (nullable; null = non-PO bill), `billDate`,
  `dueDate`, `status`, `subtotal`, `total`, `amountPaid` (derived from
  allocations, persisted for querying), `journalEntryId` (set at approval),
  `notes`, `createdAt`, `createdBy`.
- Status: `DRAFT → APPROVED → PARTIALLY_PAID → PAID`; `CANCELLED` reachable
  from `DRAFT` and from `APPROVED` only while `amountPaid = 0`.

### BillLine (`ap_bill_lines`)
- Common: `id`, `billId`, `description`, `lineTotal`.
- PO bill lines: `productId`, `purchaseOrderItemId`, `quantity`, `unitCost`.
- Non-PO bill lines: `accountId` (GL expense account), `amount`.
- A bill is either PO-backed (every line references a PO item) or non-PO
  (every line references an account); mixing is rejected.

### Payment (`ap_payments`) and PaymentAllocation (`ap_payment_allocations`)
- Payment: `id`, `paymentNumber` (generated, e.g. `PAY-2026-0001`),
  `supplierId`, `paymentDate`, `method` enum (`CASH`, `BANK`, `CHECK`),
  `reference`, `total`, `journalEntryId`, `createdAt`, `createdBy`.
- Allocation: `paymentId`, `billId`, `amount`. Sum of allocations must equal
  the payment total; each allocation ≤ that bill's remaining amount due; all
  allocated bills must belong to the payment's supplier and be `APPROVED` or
  `PARTIALLY_PAID`.

## Three-way match (enforced at bill approval; PO bills only)

1. The referenced PO must be in `RECEIVED` status.
2. Per PO item: cumulative billed quantity across all non-cancelled bills of
   that PO (including this one) must not exceed the billable quantity — the PO
   item quantity minus quantities on purchase returns for that PO/product —
   otherwise approval fails with a clear error naming the product. Hard block.
   (A purchase return created after billing can leave a bill over-billed;
   that is surfaced in the bill detail, not retroactively blocked — resolving
   it via supplier debit memos is out of scope.)
3. Price variance: for each line, `(bill unitCost − PO unitCost) × quantity`
   accumulates into the PPV posting. Never blocks.
4. The bill's supplier must match the PO's supplier.

## GL postings

New `JournalSourceType` values: `AP_BILL`, `AP_PAYMENT`.
New `AccountMappingKey` values: `ACCOUNTS_PAYABLE`, `PURCHASE_PRICE_VARIANCE`,
`CASH`, `BANK` — each seeded in `FinanceBootstrapConfig` with chart-of-accounts
entries (idempotent, consistent with existing bootstrap seeding).

- PO bill approval:
  Dr GOODS_RECEIVED_NOT_INVOICED at PO cost × billed quantity;
  Dr or Cr PURCHASE_PRICE_VARIANCE for the net variance;
  Cr ACCOUNTS_PAYABLE for the bill total. (Entry balances by construction.)
- Non-PO bill approval: Dr each line's expense account; Cr ACCOUNTS_PAYABLE.
- Payment: Dr ACCOUNTS_PAYABLE for the total; Cr CASH or BANK per method
  (CHECK posts to BANK).
- Cancelling an approved, unpaid bill reverses its journal entry via the
  existing `LedgerPostingService.reverse()` and detaches billed-quantity
  consumption from the match.
- Postings happen in the same transaction as the status change; a posting
  failure rolls back the document change (same rule as inventory postings).

## Backend surface

### Port
`PurchaseOrderReader` (defined in `modules/finance/application/port`):
returns PO header (id, poNumber, supplierId, status) and lines (poItemId,
productId, quantity, returnedQuantity, unitCost). Implemented by an adapter in
the inventory module over the existing PO and purchase-return repositories.

### API — `ApController`, base path `/api/finance/ap`
- `POST /bills` (create draft), `PUT /bills/{id}` (edit draft),
  `POST /bills/{id}/approve`, `POST /bills/{id}/cancel`,
  `GET /bills` (filters: supplier, status, PO; paged), `GET /bills/{id}`.
- `POST /payments`, `GET /payments` (filters: supplier; paged),
  `GET /payments/{id}`.
- `GET /aging`: per-supplier open balance bucketed by due date
  (current / 1–30 / 31–60 / 61–90 / 90+ days, Asia/Manila "today").

### Security
New permissions `AP_READ`, `AP_WRITE` in `PermissionCatalog`, granted to the
FINANCE role (and ADMIN) via the existing role bootstrap. Endpoints guarded
like the existing `FinanceController`.

## Frontend (erp-frontend)

Extend the existing `/finance` page pattern:
- **Bills tab**: list with status/supplier filters; create-draft dialog with a
  PO picker (received, not fully billed POs) that pre-fills lines and shows
  remaining billable quantity; non-PO mode with account picker per line;
  approve and cancel actions; bill detail with journal entry link.
- **Payments tab**: create payment for a supplier, allocate across that
  supplier's open bills (defaults to oldest-first fill); payment list.
- **Aging widget** on the finance page using `GET /aging`.
- Same structure as existing finance code: `src/api/finance.api.ts` additions
  (or `ap.api.ts`), `src/types/finance.ts` additions, components under
  `components/finance/`, permission gating via `src/lib/permissions.ts`.

## Error handling

- Validation failures (mixed line types, over-billing, supplier mismatch,
  allocation > amount due, unbalanced allocations, approving a non-DRAFT bill,
  paying a non-approved bill, cancelling a paid bill) → 400 with a message
  naming the offending line/product/bill.
- Posting failures roll back the whole operation.
- Fiscal-period checks are whatever `LedgerPostingService.post()` already
  enforces; AP adds none of its own.

## Testing

Service-level unit tests in the style of `LedgerPostingServiceTest`:
- Match: over-billing blocked (single bill and cumulative across bills);
  supplier mismatch blocked; PO-not-received blocked.
- PPV: variance posts Dr when billed above PO cost, Cr when below; zero
  variance omits the PPV line.
- Non-PO: expense lines post to their accounts; mixed-type bill rejected.
- Payments: allocation validation; status transitions APPROVED →
  PARTIALLY_PAID → PAID; amountPaid arithmetic; CASH vs BANK posting.
- Cancellation: reversal entry created; billed quantity released; cancelling
  a paid/partially-paid bill rejected.
Controller tests following the existing controller-test pattern for the new
endpoints and permission checks.

## Out of scope (later roadmap steps)

Partial receipts/GRN, payment runs and approvals, VAT input-tax split and BIR
forms, withholding tax, supplier statements and debit memos, multi-currency.
