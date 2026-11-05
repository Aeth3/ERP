# AP Bills & Payments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Supplier bills (PO-backed with 3-way match, and non-PO expense bills) and payments, posted to the general ledger, with an AP aging view.

**Architecture:** AP lives inside `modules/finance` next to the GL. Bills/payments post via the existing `LedgerPostingService.post(JournalEntry)`. Finance reads PO data through a new `PurchaseOrderReader` port implemented by an adapter in the inventory module (mirror of the `InventoryLedgerPoster` pattern).

**Tech Stack:** Spring Boot 4 / Java (backend, `D:\Projects\maiu\ERP`), Next.js pages + React Query (frontend, `D:\Projects\maiu\erp-frontend`). Tests: JUnit 5 with hand-rolled in-memory repositories (see `LedgerPostingServiceTest`), MockMvc controller tests.

**Spec:** `docs/superpowers/specs/2026-11-05-ap-bills-payments-design.md`

## Global Constraints

- Money is PHP, VAT-inclusive; `BigDecimal` with scale 2, `RoundingMode.HALF_UP` (match `LedgerPostingService`).
- Business time zone `Asia/Manila` for any "today" (aging buckets).
- Flyway does not run: every new table needs BOTH an idempotent migration (`CREATE TABLE IF NOT EXISTS`, PostgreSQL) AND JPA entities so `ddl-auto=update` creates it. Never edit existing migrations.
- Seed data goes through `FinanceSetupService.ensureDefaults()` (idempotent), called from `FinanceBootstrapConfig`.
- Follow the finance module layering exactly: `domain/model` (plain classes, getters/setters), `domain/enums`, `domain/repository` (interfaces), `infrastructure/persistence/{entity,impl,jpa}`, `application/service`, `application/dto`, `presentation/controller`.
- Posted journal entries are never edited, only reversed via `LedgerPostingService.reverse(UUID entryId, LocalDate reversalDate, String memo, String actor)`.
- Exceptions: `BadRequestException`, `NotFoundException`, `ConflictException` from `com.maiu.erp.shared.exception`.
- Commit after each task in the repo it touches; messages end with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.

## Review Focus

1. Zero/negative quantity or amount on a bill line → rejected at draft save, not at approval. (Test in Task 4.)
2. Approval when the fiscal period is closed → `LedgerPostingService` throws and the bill must stay DRAFT (same transaction). (Test in Task 4.)
3. Duplicate supplier invoice number for the same supplier → `ConflictException`; same number for a different supplier is fine. (Test in Task 4.)
4. Payment allocations that don't sum to the payment total, or exceed a bill's remaining due (including a second concurrent-ish payment) → rejected whole. (Test in Task 5.)
5. Aging for a bill with no `dueDate` → treat `billDate` as due date rather than NPE or silent omission. (Test in Task 6.)

---

### Task 1: Enums, account mappings, bootstrap seeding (backend)

**Files:**
- Modify: `src/main/java/com/maiu/erp/modules/finance/domain/enums/JournalSourceType.java`
- Modify: `src/main/java/com/maiu/erp/modules/finance/domain/enums/AccountMappingKey.java`
- Modify: `src/main/java/com/maiu/erp/modules/finance/application/service/FinanceSetupService.java`
- Test: `src/test/java/com/maiu/erp/modules/finance/application/service/FinanceSetupServiceTest.java` (extend if present; create otherwise)

**Interfaces:**
- Produces: `JournalSourceType.AP_BILL`, `JournalSourceType.AP_PAYMENT`; `AccountMappingKey.ACCOUNTS_PAYABLE`, `PURCHASE_PRICE_VARIANCE`, `CASH`, `BANK`. Default accounts seeded: `2100 Accounts Payable` (LIABILITY), `5150 Purchase Price Variance` (EXPENSE), `1000 Cash on Hand` (ASSET), `1010 Cash in Bank` (ASSET) — skip any code that already exists in `DEFAULT_ACCOUNTS`; reuse existing codes if 1000/1010/2100 are taken by equivalent accounts (check `DEFAULT_ACCOUNTS` first and keep one source of truth).

- [ ] **Step 1: Write failing test** — `ensureDefaults_seedsApAccountsAndMappings`: after `ensureDefaults()`, `accountMappingRepository.findAccountId(key)` is present for all four new keys, and running `ensureDefaults()` twice does not duplicate accounts.
- [ ] **Step 2: Run it, expect FAIL** — `./gradlew test --tests '*FinanceSetupServiceTest*'` (compile error on new enum constants is an acceptable first failure).
- [ ] **Step 3: Add the enum constants and extend `DEFAULT_ACCOUNTS` / `DEFAULT_MAPPINGS`** in `FinanceSetupService`.
- [ ] **Step 4: Run test, expect PASS**, plus `./gradlew test --tests '*LedgerPostingServiceTest*'` still green.
- [ ] **Step 5: Commit** — `feat(finance): AP account mappings and journal source types`

### Task 2: AP domain models, repositories, persistence (backend)

**Files:**
- Create: `domain/enums/BillStatus.java` (`DRAFT, APPROVED, PARTIALLY_PAID, PAID, CANCELLED`), `domain/enums/PaymentMethod.java` (`CASH, BANK, CHECK`) under `modules/finance`
- Create: `domain/model/Bill.java`, `BillLine.java`, `Payment.java`, `PaymentAllocation.java` (fields per spec; plain getter/setter classes like `JournalEntry`)
- Create: `domain/repository/BillRepository.java`, `PaymentRepository.java`
- Create: `infrastructure/persistence/entity/BillEntity.java`, `BillLineEntity.java`, `PaymentEntity.java`, `PaymentAllocationEntity.java`; `impl/BillRepositoryImpl.java`, `PaymentRepositoryImpl.java`; `jpa/BillJpaRepository.java`, `PaymentJpaRepository.java`
- Create: `src/main/resources/db/migration/V20261105_01__accounts_payable.sql` (idempotent; tables `ap_bills`, `ap_bill_lines`, `ap_payments`, `ap_payment_allocations`; unique index on `(supplier_id, bill_number)` where status <> 'CANCELLED')
- Test: `src/test/java/com/maiu/erp/modules/finance/infrastructure/persistence/impl/BillPersistenceTest.java` (same style as `JournalEntryPersistenceTest`)

**Interfaces:**
- Produces:
  - `BillRepository`: `Bill save(Bill bill)`, `Optional<Bill> findById(UUID id)`, `List<Bill> findByPurchaseOrderId(UUID poId)`, `List<Bill> search(UUID supplierId, BillStatus status, UUID purchaseOrderId)`, `boolean existsBySupplierIdAndBillNumberAndStatusNot(UUID supplierId, String billNumber, BillStatus excluded)`, `List<Bill> findOpen()` (status APPROVED or PARTIALLY_PAID).
  - `PaymentRepository`: `Payment save(Payment p)`, `Optional<Payment> findById(UUID id)`, `List<Payment> findBySupplierId(UUID supplierId)`, `List<Payment> findAll()`.
  - `Bill` carries `List<BillLine> lines`; `Payment` carries `List<PaymentAllocation> allocations`; save cascades lines/allocations (same approach as `JournalEntry`→`JournalLine`).
  - `BillLine` fields: `id, billId, productId, purchaseOrderItemId, quantity, unitCost, accountId, description, lineTotal` (nullable per bill kind).

- [ ] **Step 1: Write failing persistence test** — round-trips a PO bill with 2 lines and a payment with 1 allocation; asserts all fields and child ordering survive.
- [ ] **Step 2: Run, expect FAIL** (classes missing).
- [ ] **Step 3: Implement models, entities, repositories, migration** following the `JournalEntry` stack file-for-file.
- [ ] **Step 4: Run test, expect PASS.**
- [ ] **Step 5: Commit** — `feat(finance): AP bill and payment domain + persistence`

### Task 3: PurchaseOrderReader port + inventory adapter (backend)

**Files:**
- Create: `modules/finance/application/port/PurchaseOrderReader.java`
- Create: `modules/inventory/infrastructure/adapter/FinancePurchaseOrderReader.java` (Spring `@Component`)
- Test: `src/test/java/com/maiu/erp/modules/inventory/infrastructure/adapter/FinancePurchaseOrderReaderTest.java`

**Interfaces:**
- Produces:
  ```java
  public interface PurchaseOrderReader {
      Optional<PoSnapshot> find(UUID purchaseOrderId);
      record PoSnapshot(UUID id, String poNumber, UUID supplierId, String status, List<PoLine> lines) {}
      record PoLine(UUID poItemId, UUID productId, BigDecimal quantity, BigDecimal returnedQuantity, BigDecimal unitCost) {}
  }
  ```
  `status` is the PO status name (`"RECEIVED"` etc.) so finance does not import inventory enums.
- Consumes: inventory's `PurchaseOrderRepository`, PO item repository, and `PurchaseReturnRepository` (sum returned quantity per product for the PO's returns).

- [ ] **Step 1: Write failing test** — adapter returns snapshot with `returnedQuantity` summed from two purchase returns for the same product; empty Optional for unknown id.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement port + adapter.**
- [ ] **Step 4: Run, expect PASS.**
- [ ] **Step 5: Commit** — `feat: PurchaseOrderReader port for finance AP`

### Task 4: ApBillService — drafts, 3-way match, approval posting, cancel (backend)

**Files:**
- Create: `modules/finance/application/service/ApBillService.java`, `application/dto/BillDto.java` (+ request records in the service or dto package, following `FinanceController`'s request style)
- Test: `src/test/java/com/maiu/erp/modules/finance/application/service/ApBillServiceTest.java` (in-memory repos; reuse the in-memory account/mapping/journal/period fakes from `LedgerPostingServiceTest` — extract them to a shared test support class if needed)

**Interfaces:**
- Consumes: Task 2 repositories, Task 3 `PurchaseOrderReader`, `LedgerPostingService`, `AccountMappingRepository`, `CurrentActor` (existing finance helper for createdBy).
- Produces (used by Tasks 5–6):
  ```java
  Bill createDraft(CreateBillRequest req)        // validates line shape, positive qty/amounts, duplicate bill number
  Bill updateDraft(UUID id, CreateBillRequest req) // DRAFT only
  Bill approve(UUID id)                           // match + post; sets journalEntryId, status APPROVED
  Bill cancel(UUID id)                            // DRAFT: just cancel; APPROVED+amountPaid=0: reverse JE then cancel
  Bill get(UUID id); List<Bill> search(UUID supplierId, BillStatus status, UUID poId)
  BigDecimal remainingDue(Bill bill)              // total - amountPaid
  ```
  `CreateBillRequest(UUID supplierId, UUID purchaseOrderId, String billNumber, LocalDate billDate, LocalDate dueDate, String notes, List<LineReq> lines)`; `LineReq(UUID purchaseOrderItemId, BigDecimal quantity, BigDecimal unitCost, UUID accountId, BigDecimal amount, String description)`.
- Match rule (approve, PO bills): PO status must be `RECEIVED`; PO supplier must equal bill supplier; per PO item, billed qty across non-CANCELLED bills of that PO + this bill ≤ `quantity − returnedQuantity`.
- Posting (approve): PO bill → Dr GRNI (Σ PO unitCost × qty), Dr/Cr PPV (Σ (bill − PO unitCost) × qty, net; omit line if zero), Cr ACCOUNTS_PAYABLE (bill total); non-PO bill → Dr each line's accountId, Cr AP. `sourceType = AP_BILL`, `sourceId = bill.id`, entryDate = billDate.

- [ ] **Step 1: Write failing tests** (names fix behavior):
  - `createDraft_rejectsMixedLineTypes`, `createDraft_rejectsNonPositiveQuantityOrAmount`, `createDraft_rejectsDuplicateBillNumberForSupplier` (and allows same number for another supplier)
  - `approve_blocksWhenPoNotReceived`, `approve_blocksSupplierMismatch`, `approve_blocksOverBillingNetOfReturns` (cumulative across two bills)
  - `approve_postsGrniPpvAndAp` (billed above PO cost → PPV debit), `approve_postsPpvCreditWhenBelowPoCost`, `approve_omitsPpvLineWhenExactMatch`, `approve_nonPoBillPostsExpenseLines`
  - `approve_rollsBackWhenPeriodClosed` (posting throws → bill still DRAFT, no journalEntryId)
  - `cancel_approvedUnpaidReversesJournal`, `cancel_rejectedWhenPaid`
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement `ApBillService`.**
- [ ] **Step 4: Run, expect PASS** (whole finance test package green).
- [ ] **Step 5: Commit** — `feat(finance): AP bills with 3-way match and GL posting`

### Task 5: ApPaymentService (backend)

**Files:**
- Create: `modules/finance/application/service/ApPaymentService.java`, dto records
- Test: `src/test/java/com/maiu/erp/modules/finance/application/service/ApPaymentServiceTest.java`

**Interfaces:**
- Consumes: Task 2 repos, Task 4 `ApBillService.remainingDue`, `LedgerPostingService`, `AccountMappingRepository`.
- Produces: `Payment create(CreatePaymentRequest req)`; `CreatePaymentRequest(UUID supplierId, LocalDate paymentDate, PaymentMethod method, String reference, List<AllocationReq> allocations)` with `AllocationReq(UUID billId, BigDecimal amount)`; payment total = Σ allocations. `paymentNumber` format `PAY-yyyyMM-<4-digit seq>` (mirror `generateEntryNumber`).
- Rules: every bill APPROVED/PARTIALLY_PAID, same supplier, allocation > 0 and ≤ remaining due; updates each bill's `amountPaid` and status (PARTIALLY_PAID / PAID when remaining hits 0). Posting: Dr AP total / Cr CASH (method CASH) or BANK (BANK and CHECK); `sourceType = AP_PAYMENT`, `sourceId = payment.id`.

- [ ] **Step 1: Write failing tests** — `create_rejectsAllocationExceedingRemainingDue`, `create_rejectsBillOfOtherSupplier`, `create_rejectsUnapprovedBill`, `create_partialPaymentSetsPartiallyPaid`, `create_fullPaymentAcrossTwoBillsSetsPaid`, `create_checkPostsToBank`, `create_secondPaymentCannotOverpay` (two sequential payments where the second exceeds what the first left).
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run, expect PASS.**
- [ ] **Step 5: Commit** — `feat(finance): AP payments with bill allocation`

### Task 6: Aging, ApController, permissions (backend)

**Files:**
- Create: `modules/finance/presentation/controller/ApController.java` (`@RequestMapping("/finance/ap")`)
- Create: `modules/finance/application/service/ApAgingService.java`
- Modify: `modules/identity/application/security/PermissionCatalog.java` (add `FINANCE_AP_READ = "finance.ap.read"`, `FINANCE_AP_MANAGE = "finance.ap.manage"`; register in the catalog list and in `DefaultRoleService`/`RoleBootstrapConfig` grants for FINANCE and ADMIN, same way `FINANCE_JOURNAL_MANAGE` is wired)
- Test: `src/test/java/com/maiu/erp/modules/finance/presentation/controller/ApControllerTest.java` (existing controller-test pattern), aging cases in `ApAgingServiceTest`

**Interfaces:**
- Consumes: Tasks 4–5 services.
- Produces endpoints: `POST /finance/ap/bills`, `PUT /finance/ap/bills/{id}`, `POST /finance/ap/bills/{id}/approve`, `POST /finance/ap/bills/{id}/cancel`, `GET /finance/ap/bills?supplierId&status&purchaseOrderId`, `GET /finance/ap/bills/{id}`, `POST /finance/ap/payments`, `GET /finance/ap/payments?supplierId`, `GET /finance/ap/payments/{id}`, `GET /finance/ap/aging`. `/finance/**` is already FINANCE-role-gated in `SecurityConfig`; method-level checks use the new permissions like `FinanceController` does.
- `ApAgingService.aging()` → per supplier: open balance in buckets CURRENT, D1_30, D31_60, D61_90, OVER_90 by days past due at `LocalDate.now(Asia/Manila)`; `dueDate == null` → use `billDate`.

- [ ] **Step 1: Write failing tests** — controller: create→approve→pay happy path, 403 without permission, 400 surfaces match error message; aging: boundary at exactly 30/31 days, null dueDate falls back to billDate, paid bills excluded.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement controller + aging + permission wiring.**
- [ ] **Step 4: Run full backend suite** — `./gradlew test`, expect green.
- [ ] **Step 5: Commit** — `feat(finance): AP endpoints, aging report, permissions`

### Task 7: Frontend types + API client (erp-frontend)

**Files:**
- Create: `src/api/ap.api.ts`, extend `src/types/finance.ts` (Bill, BillLine, Payment, Allocation, AgingRow types matching Task 6 DTOs; `BillStatus`, `PaymentMethod` unions)
- Modify: `src/lib/permissions.ts` (add `finance.ap.read` / `finance.ap.manage` constants following existing entries)

**Interfaces:**
- Produces: `getBills(params)`, `getBill(id)`, `createBill(payload)`, `updateBill(id, payload)`, `approveBill(id)`, `cancelBill(id)`, `createPayment(payload)`, `getPayments(params)`, `getAging()` — same fetch style as `finance.api.ts`.

- [ ] **Step 1: Implement types + api functions** (follow `finance.api.ts` exactly; no component changes yet).
- [ ] **Step 2: Verify** — `npm run build` (or the repo's typecheck script) passes.
- [ ] **Step 3: Commit** — `feat(finance): AP types and API client`

### Task 8: Frontend Bills/Payments tabs + aging widget (erp-frontend)

**Files:**
- Create: `components/finance/bills-tab.tsx`, `components/finance/payments-tab.tsx`, `components/finance/ap-aging-card.tsx`
- Modify: `pages/finance.tsx` (register the two new tabs + aging card, gate on `finance.ap.read`), `components/layout/sidebar.tsx` only if finance nav labels need it

**Interfaces:**
- Consumes: Task 7 API. Follow `journal-tab.tsx` for list/detail/dialog structure and `finance-format.ts` for money/date formatting.
- Behavior pinned by spec: bill dialog has PO mode (PO picker of RECEIVED POs; lines pre-filled from PO with remaining billable qty shown; unit cost editable) and non-PO mode (account picker + amount per line); approve/cancel actions with confirm; payment dialog picks supplier then allocates across open bills oldest-due-first by default; aging card shows the five buckets per supplier.

- [ ] **Step 1: Implement components + wiring.**
- [ ] **Step 2: Verify** — `npm run build` passes; manual smoke against running backend if available (list loads, create draft, approve, pay).
- [ ] **Step 3: Commit** — `feat(finance): AP bills, payments and aging UI`
