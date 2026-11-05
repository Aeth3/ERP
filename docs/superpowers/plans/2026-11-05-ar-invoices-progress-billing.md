# AR Invoices & Progress Billing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Customer invoices (SO-backed and project progress billings with retention), receipts, and AR aging, posting to the GL.

**Architecture:** AR lives in `modules/finance` mirroring the AP bill/payment stack exactly: domain models + repository interfaces, JPA persistence, services that validate then post via `LedgerPostingService`, a `/finance/ar` controller, and ports (`SalesOrderReader`, `ProjectBillingReader`) implemented by inventory/project adapters. Frontend adds Invoices/Receipts/AR Aging tabs to `/finance`.

**Tech Stack:** Spring Boot 4, JPA/Hibernate (ddl-auto=update + idempotent Flyway-style SQL files that never run), JUnit 5 + in-memory fakes + @DataJpaTest/H2 + standalone MockMvc; Next.js + React Query frontend.

**Spec:** `docs/superpowers/specs/2026-11-05-ar-invoices-progress-billing-design.md`

## Global Constraints

- Money: PHP, VAT-inclusive, BigDecimal scale ≤ 2, HALF_UP; business zone `finance.time-zone` (Asia/Manila).
- Migrations never run (no Flyway starter): every table/column needs BOTH an idempotent SQL migration AND matching JPA entity columns; never edit old migrations.
- Posted journal entries are only reversed via `LedgerPostingService.reverse(id, date, memo, actor)`, never edited.
- Transactions: `jakarta.transaction.Transactional`; actor via `CurrentActor.name()`.
- Finance reads other modules only through `application/port` interfaces; adapters live in the owning module's `infrastructure/adapter`.
- Numbers: `ARI-yyyyMM-nnnn` / `RCT-yyyyMM-nnnn`, prefix-count + exists skip loop, unique column backstop (PAY pattern).
- Backend test command: `.\gradlew.bat test` in `D:\Projects\maiu\ERP`. Frontend: `npx tsc --noEmit` and `npx next build` in `D:\Projects\maiu\erp-frontend`.
- Commits end with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.

## Review Focus

1. Null `retentionRate` on a project → progress billing proceeds with zero retention and no RETENTION_RECEIVABLE journal line. (Test in Task 5.)
2. Receipt allocation against a DRAFT or CANCELLED invoice → rejected "not open for receipt". (Test in Task 6.)
3. `releaseRetention` when releasable is zero (no billings, or all retention already released) → clear BadRequest, no invoice created. (Test in Task 5.)
4. CreateInvoiceRequest with fields of the wrong kind (PROGRESS_BILLING with salesOrderId set, SALES_ORDER with projectId set) → rejected at draft. (Test in Task 4.)
5. SO invoice drafted while SO is CONFIRMED then approved before shipping → approve blocks with "not shipped" error naming the SO. (Test in Task 5.)

---

### Task 1: Enums, account mappings, migration, Project contract fields

**Files:**
- Create: `src/main/java/com/maiu/erp/modules/finance/domain/enums/InvoiceKind.java`, `InvoiceStatus.java`
- Create: `src/main/resources/db/migration/V20261105_02__accounts_receivable.sql`
- Modify: `modules/finance/domain/enums/AccountMappingKey.java`, `JournalSourceType.java`
- Modify: `modules/finance/application/service/FinanceSetupService.java` (new mappings in ensureDefaults)
- Modify: `modules/project/domain/model/Project.java`, `modules/project/infrastructure/persistence/entity/ProjectEntity.java` (+ its impl mapping), project create/update service + DTOs (add the two fields wherever budgetAmount flows)
- Test: `src/test/java/.../finance/application/service/FinanceSetupServiceTest.java`

**Interfaces:**
- Produces: `InvoiceKind { SALES_ORDER, PROGRESS_BILLING, RETENTION_RELEASE }`; `InvoiceStatus { DRAFT, APPROVED, PARTIALLY_PAID, PAID, CANCELLED }`; `AccountMappingKey.ACCOUNTS_RECEIVABLE/RETENTION_RECEIVABLE/SALES_REVENUE/CONTRACT_REVENUE`; `JournalSourceType.AR_INVOICE/AR_RECEIPT`; `Project.getContractAmount()/getRetentionRate()` (BigDecimal, nullable).

- [ ] **Step 1: Write failing test** — extend `FinanceSetupServiceTest`: `ensureDefaultsMapsArAccounts` asserting mappings ACCOUNTS_RECEIVABLE→code "1100", RETENTION_RECEIVABLE→"1110", SALES_REVENUE→"4100", CONTRACT_REVENUE→"4000" exist after `ensureDefaults()` (accounts already in CoA — account count assertion stays unchanged; mapping count grows by 4).
- [ ] **Step 2: Run it** — `.\gradlew.bat test --tests "*FinanceSetupServiceTest"` Expected: FAIL (enum constants missing → compile error counts as the failing state; fix compile by adding enums first if needed, then assert fails on missing mappings).
- [ ] **Step 3: Implement** — add the two new enums, the four AccountMappingKey constants, the two JournalSourceType constants, and the mapping entries in `ensureDefaults()` keyed to existing account codes (follow the ACCOUNTS_PAYABLE pattern).
- [ ] **Step 4: Run it** — same command. Expected: PASS, whole FinanceSetupServiceTest green.
- [ ] **Step 5: Add Project fields** — `contractAmount` numeric(14,2) nullable, `retentionRate` numeric(5,2) nullable on model, entity, impl mapping, and the project create/update request/response DTOs and service copy-over (same plumbing as budgetAmount).
- [ ] **Step 6: Write migration** — `V20261105_02__accounts_receivable.sql`, idempotent (`CREATE TABLE IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`): `ar_invoices`, `ar_invoice_lines`, `ar_receipts`, `ar_receipt_allocations` (columns per spec data model, UUID PKs, unique index on `ar_invoices.invoice_number` and `ar_receipts.receipt_number`), plus `projects.contract_amount`, `projects.retention_rate`.
- [ ] **Step 7: Run full project tests** — `.\gradlew.bat test` Expected: all pass.
- [ ] **Step 8: Commit** — `feat: AR enums, account mappings, migration, project contract fields`

### Task 2: Domain models, repositories, persistence

**Files:**
- Create: `modules/finance/domain/model/Invoice.java`, `InvoiceLine.java`, `Receipt.java`, `ReceiptAllocation.java`
- Create: `modules/finance/domain/repository/InvoiceRepository.java`, `ReceiptRepository.java`
- Create: `infrastructure/persistence/entity/InvoiceEntity.java`, `InvoiceLineEntity.java`, `ReceiptEntity.java`, `ReceiptAllocationEntity.java`; `jpa/InvoiceJpaRepository.java`, `ReceiptJpaRepository.java`; `impl/InvoiceRepositoryImpl.java`, `ReceiptRepositoryImpl.java`
- Test: `src/test/java/.../finance/infrastructure/persistence/InvoicePersistenceTest.java` (@DataJpaTest, H2, pattern of BillPersistenceTest)

**Interfaces:**
- Consumes: Task 1 enums.
- Produces (fields per spec tables; plain getters/setters):
  - `Invoice`: id, invoiceNumber, kind, customerId, salesOrderId, projectId, invoiceDate, dueDate, status, subtotal, retentionAmount, total, amountReceived, journalEntryId, notes, createdBy, createdAt, `List<InvoiceLine> lines`.
  - `InvoiceLine`: id, invoiceId, lineNumber, salesOrderItemId, productId, description, quantity, unitPrice, lineTotal.
  - `Receipt`: id, receiptNumber, customerId, receiptDate, method (PaymentMethod), reference, total, journalEntryId, createdBy, createdAt, `List<ReceiptAllocation> allocations`; `ReceiptAllocation`: id, receiptId, invoiceId, amount.
  - `InvoiceRepository`: `Invoice save(Invoice)`, `Optional<Invoice> findById(UUID)`, `Optional<Invoice> findByIdForUpdate(UUID)` (@Lock PESSIMISTIC_WRITE in JPA), `List<Invoice> findBySalesOrderId(UUID)`, `List<Invoice> findByProjectId(UUID)`, `List<Invoice> search(UUID customerId, InvoiceStatus status, InvoiceKind kind)` (all nullable, null-safe @Query), `boolean existsByInvoiceNumber(String)`, `List<Invoice> findOpen()` (status APPROVED or PARTIALLY_PAID).
  - `ReceiptRepository`: `Receipt save`, `Optional<Receipt> findById(UUID)`, `List<Receipt> findByCustomerId(UUID)`, `List<Receipt> findAll()`, `boolean existsByReceiptNumber(String)`.
- Entities: tables `ar_invoices` etc.; OneToMany EAGER cascade ALL orphanRemoval, `@OrderBy("lineNumber")` on lines (allocations unordered).

- [ ] **Step 1: Write failing test** — `InvoicePersistenceTest`: `savesAndReloadsInvoiceWithLines` (round-trip all fields incl. two ordered lines), `searchIsNullSafe` (search(null,null,null) returns saved rows; filter by kind works), `findOpenReturnsApprovedAndPartiallyPaidOnly`, `savesAndReloadsReceiptWithAllocations`.
- [ ] **Step 2: Run it** — `.\gradlew.bat test --tests "*InvoicePersistenceTest"` Expected: compile FAIL (types missing).
- [ ] **Step 3: Implement** models, repositories, entities, JPA repos, impls per Produces block (copy the Bill/Payment persistence layout).
- [ ] **Step 4: Run it** — Expected: PASS. Then `.\gradlew.bat test` green.
- [ ] **Step 5: Commit** — `feat: AR invoice and receipt domain + persistence`

### Task 3: Ports and adapters

**Files:**
- Create: `modules/finance/application/port/SalesOrderReader.java`, `ProjectBillingReader.java`
- Create: `modules/inventory/infrastructure/adapter/FinanceSalesOrderReader.java`
- Create: `modules/project/infrastructure/adapter/FinanceProjectBillingReader.java`
- Test: `src/test/java/.../inventory/infrastructure/adapter/FinanceSalesOrderReaderTest.java`

**Interfaces:**
- Produces:
  - `SalesOrderReader`: `Optional<SoSnapshot> find(UUID salesOrderId)`; `record SoSnapshot(UUID id, String soNumber, UUID customerId, String status, List<SoLine> lines)`; `record SoLine(UUID soItemId, UUID productId, String productName, BigDecimal quantity, BigDecimal returnedQuantity, BigDecimal unitPrice)`.
  - `ProjectBillingReader`: `Optional<ProjectSnapshot> find(UUID projectId)`; `record ProjectSnapshot(UUID id, String projectCode, String projectName, UUID customerId, String status, BigDecimal contractAmount, BigDecimal retentionRate)`.
- `FinanceSalesOrderReader` constructor: SalesOrderRepository, SalesOrderItemRepository, SalesReturnRepository, SalesReturnItemRepository, ProductRepository. Returns are product-level: sum returned qty per product across the SO's returns, allocate greedily across SO items of that product in item order (same algorithm as `FinancePurchaseOrderReader` — follow it).
- `FinanceProjectBillingReader` constructor: ProjectRepository; maps status via `name()`.

- [ ] **Step 1: Write failing test** — `FinanceSalesOrderReaderTest` (Mockito mocks of the five repos, pattern of FinancePurchaseOrderReaderTest): `mapsSnapshotFields`, `allocatesReturnsGreedilyAcrossDuplicateProductItems` (two items of same product qty 5 and 5, returns total 7 → returnedQuantity 5 and 2), `emptyWhenSalesOrderMissing`.
- [ ] **Step 2: Run it** — Expected: compile FAIL.
- [ ] **Step 3: Implement** ports and both adapters (`@Component`).
- [ ] **Step 4: Run it** — PASS; full `.\gradlew.bat test` green.
- [ ] **Step 5: Commit** — `feat: sales order and project billing reader ports`

### Task 4: ArInvoiceService — drafts, invoiceable lines, billing summary

**Files:**
- Create: `modules/finance/application/service/ArInvoiceService.java`
- Create test fakes: `src/test/java/.../finance/testsupport/InMemoryInvoiceRepository.java` (with `getForUpdateCalls()`), reuse existing fakes otherwise
- Test: `src/test/java/.../finance/application/service/ArInvoiceServiceTest.java`

**Interfaces:**
- Consumes: Task 2 repositories/models, Task 3 ports, existing LedgerPostingService + AccountMappingRepository + InMemory fakes.
- Produces on `ArInvoiceService` (constructor: InvoiceRepository, SalesOrderReader, ProjectBillingReader, LedgerPostingService, AccountMappingRepository, `@Value("${finance.time-zone}") String timeZone`):
  - `record CreateInvoiceRequest(InvoiceKind kind, UUID customerId, UUID salesOrderId, UUID projectId, LocalDate invoiceDate, LocalDate dueDate, String notes, List<LineReq> lines)`; `record LineReq(UUID salesOrderItemId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, String description)`.
  - `Invoice createDraft(CreateInvoiceRequest)`, `Invoice updateDraft(UUID, CreateInvoiceRequest)`, `Invoice get(UUID)`, `List<Invoice> search(UUID customerId, InvoiceStatus status, InvoiceKind kind)`.
  - `record InvoiceableLine(UUID soItemId, UUID productId, String productName, BigDecimal shippedQuantity, BigDecimal returnedQuantity, BigDecimal invoicedQuantity, BigDecimal remainingQuantity, BigDecimal unitPrice)`; `List<InvoiceableLine> invoiceableLines(UUID salesOrderId)` (invoicedQuantity = non-CANCELLED invoices; shippedQuantity = ordered qty — whole-SO shipping).
  - `record ProjectBillingSummary(UUID projectId, String projectCode, BigDecimal contractAmount, BigDecimal retentionRate, BigDecimal billedToDate, BigDecimal retentionHeld, BigDecimal retentionReleased, BigDecimal remainingContract)`; `ProjectBillingSummary projectBillingSummary(UUID projectId)`.
- Invoice number assigned at createDraft: `ARI-yyyyMM-nnnn` from invoiceDate month in `finance.time-zone`, prefix count over `search(null, null, null)` + existsByInvoiceNumber skip loop (PAY pattern).
- Draft rules (spec §ArInvoiceService): kind-specific required/forbidden ids; SALES_ORDER lines validated against snapshot membership, productId + default unitPrice copied; PROGRESS_BILLING exactly one line with positive `amount`, retentionAmount = amount × retentionRate/100 scale 2 HALF_UP (null rate → 0); customer must equal SO/project customer; quantities/amounts ≤ 2 dp; subtotal/total per spec; RETENTION_RELEASE kind rejected in createDraft/updateDraft.

- [ ] **Step 1: Write failing tests** — `ArInvoiceServiceTest` draft slice: `createsSalesOrderDraftCopyingProductAndPrice`, `rejectsLineNotOnSalesOrder`, `rejectsCustomerMismatch`, `rejectsWrongKindIds` (PROGRESS_BILLING with salesOrderId; SALES_ORDER with projectId), `rejectsThirdDecimalPlace`, `computesRetentionFromRate` (10% of 1000.555→ amount scale rejected; use 1000.55 → retention 100.06, total 900.49), `nullRetentionRateMeansZero`, `rejectsProgressBillingOnProjectWithoutContract`, `rejectsRetentionReleaseKindOnCreate`, `updateDraftOnlyWhileDraft`, `invoiceNumbersSkipTakenValues`, `invoiceableLinesSubtractsReturnsAndPriorInvoices`, `projectBillingSummaryAggregates`.
- [ ] **Step 2: Run** — Expected: compile FAIL.
- [ ] **Step 3: Implement** the draft/query half of ArInvoiceService (approve/cancel/release stubs throwing UnsupportedOperationException are acceptable until Task 5).
- [ ] **Step 4: Run** — PASS; full suite green.
- [ ] **Step 5: Commit** — `feat: AR invoice drafts, invoiceable lines, billing summary`

### Task 5: ArInvoiceService — approve, cancel, retention release, postings

**Files:**
- Modify: `ArInvoiceService.java`
- Test: extend `ArInvoiceServiceTest.java`

**Interfaces:**
- Produces: `Invoice approve(UUID id)`, `Invoice cancel(UUID id)`, `Invoice releaseRetention(UUID projectId, LocalDate invoiceDate, LocalDate dueDate)`.
- Approve (findByIdForUpdate, @Transactional): SALES_ORDER — SO status must be SHIPPED or DELIVERED; cumulative per-item invoiced qty (non-CANCELLED invoices of the SO + repeated lines in this invoice via `merge`) ≤ quantity − returnedQuantity, error names productName. PROGRESS_BILLING — cumulative subtotal of non-CANCELLED progress billings + this ≤ contractAmount. RETENTION_RELEASE — subtotal ≤ releasable excluding itself. Then post and set journalEntryId, status APPROVED.
- Postings (spec table): SALES_ORDER Dr AR total / Cr SALES_REVENUE; PROGRESS_BILLING Dr AR (subtotal−retention) + Dr RETENTION_RECEIVABLE retention / Cr CONTRACT_REVENUE subtotal, zero retention line omitted; RETENTION_RELEASE Dr AR / Cr RETENTION_RECEIVABLE. Memo `"Invoice " + invoiceNumber`, sourceType AR_INVOICE, entryDate invoiceDate.
- Cancel: DRAFT → CANCELLED; APPROVED with amountReceived 0 → `ledgerPostingService.reverse(journalEntryId, today-in-zone, "Cancel invoice " + number, CurrentActor.name())` then CANCELLED; received > 0 → BadRequest "has receipts"; CANCELLED → distinct "already cancelled".
- `releaseRetention`: releasable = Σ retentionAmount (non-CANCELLED PROGRESS_BILLING) − Σ subtotal (non-CANCELLED RETENTION_RELEASE); must be > 0 else BadRequest; creates DRAFT RETENTION_RELEASE invoice, one line `"Retention release — " + projectCode`, amount = releasable, customer from project.

- [ ] **Step 1: Write failing tests** — `approveBlocksUnshippedSalesOrder`, `approveBlocksOverInvoiceIncludingRepeatedLines`, `returnsReduceInvoiceableOnApprove`, `approveBlocksOverContractCumulative`, `postsSalesOrderInvoiceEntry` (accounts + amounts), `postsProgressBillingWithRetentionSplit`, `omitsZeroRetentionLine`, `postsRetentionReleaseEntry`, `approveLocksInvoice` (getForUpdateCalls), `cancelDraftSkipsLedger`, `cancelApprovedReverses`, `cancelRejectsWhenReceipted`, `releaseRetentionComputesReleasable` (incl. after a cancelled release restoring it), `releaseRetentionRejectsZero`, `approveReleaseRejectsWhenDoubleDrafted`.
- [ ] **Step 2: Run** — Expected: FAIL (stubs throw).
- [ ] **Step 3: Implement** approve/cancel/releaseRetention + numbering.
- [ ] **Step 4: Run** — PASS; full suite green.
- [ ] **Step 5: Commit** — `feat: AR invoice approval, cancellation, retention release with GL postings`

### Task 6: ArReceiptService and ArAgingService

**Files:**
- Create: `modules/finance/application/service/ArReceiptService.java`, `ArAgingService.java`
- Create: `src/test/java/.../finance/testsupport/InMemoryReceiptRepository.java`
- Test: `ArReceiptServiceTest.java`, `ArAgingServiceTest.java`

**Interfaces:**
- Consumes: Tasks 2 & 5.
- Produces:
  - `ArReceiptService` (ReceiptRepository, InvoiceRepository, LedgerPostingService, AccountMappingRepository): `record CreateReceiptRequest(UUID customerId, LocalDate receiptDate, PaymentMethod method, String reference, List<AllocationReq> allocations)`; `record AllocationReq(UUID invoiceId, BigDecimal amount)`; `Receipt create(CreateReceiptRequest)`, `Receipt get(UUID)`, `List<Receipt> search(UUID customerId)`. Mirror ApPaymentService exactly: validations, findByIdForUpdate, merged per-invoice allocation ≤ total − amountReceived, posting Dr CASH/BANK (CHECK→BANK) / Cr ACCOUNTS_RECEIVABLE, sourceType AR_RECEIPT, memo `"Receipt " + receiptNumber`, status roll-up PAID when amountReceived ≥ total, number `RCT-yyyyMM-nnnn` skip loop.
  - `ArAgingService` (InvoiceRepository, `@Value finance.time-zone`): `List<AgingRow> aging()`, `aging(LocalDate asOf)`; `record AgingRow(UUID customerId, BigDecimal current, BigDecimal days1To30, BigDecimal days31To60, BigDecimal days61To90, BigDecimal over90, BigDecimal total)`; buckets by ChronoUnit.DAYS from dueDate (null → invoiceDate) over findOpen remaining due.

- [ ] **Step 1: Write failing tests** — `ArReceiptServiceTest`: `createsReceiptAndRollsUpStatus`, `rejectsAllocationBeyondRemainingDue` (merged duplicates), `rejectsNonOpenInvoice` (DRAFT and CANCELLED), `rejectsCustomerMismatch`, `postsCashVsBankByMethod` (CHECK→BANK), `locksInvoicesOnCreate`, `receiptNumbersSkipTakenValues`, `partialThenFinalReceiptMarksPaid`. `ArAgingServiceTest`: `bucketsByDaysOverdue`, `fallsBackToInvoiceDateWhenNoDueDate`, `netsAmountReceived`.
- [ ] **Step 2: Run** — Expected: compile FAIL.
- [ ] **Step 3: Implement** both services.
- [ ] **Step 4: Run** — PASS; full suite green.
- [ ] **Step 5: Commit** — `feat: AR receipts and aging`

### Task 7: ArController and permissions

**Files:**
- Create: `modules/finance/presentation/controller/ArController.java`
- Modify: `modules/identity/application/security/PermissionCatalog.java`
- Test: `ArControllerTest.java` (standalone MockMvc + GlobalExceptionHandler, pattern of ApControllerTest)

**Interfaces:**
- Consumes: Tasks 4–6 services.
- Produces `@RequestMapping("/finance/ar")`: `POST /invoices` (201), `PUT /invoices/{id}`, `POST /invoices/{id}/approve`, `POST /invoices/{id}/cancel`, `GET /invoices?customerId&status&kind`, `GET /invoices/{id}`, `POST /projects/{projectId}/release-retention` (201; body `{invoiceDate, dueDate}` as a small record), `GET /invoiceable?salesOrderId`, `GET /projects/{projectId}/billing-summary`, `POST /receipts` (201), `GET /receipts?customerId`, `GET /receipts/{id}`, `GET /aging`. Domain objects returned directly (no DTO layer — AP ruling).
- PermissionCatalog adds `FINANCE_AR_READ = "finance.ar.read"`, `FINANCE_AR_MANAGE = "finance.ar.manage"` to allPermissions + financePermissions. No @PreAuthorize (role matcher covers /finance/**).

- [ ] **Step 1: Write failing tests** — `createInvoiceReturnsCreated`, `approveSurfacesMatchErrorAsBadRequest`, `listInvoicesPassesFilters` (customerId + status + kind), `releaseRetentionReturnsCreatedDraft`, `billingSummaryReturnsAggregates`, `createReceiptReturnsCreated`, `agingReturnsRows`, `invoiceableReturnsRemaining`.
- [ ] **Step 2: Run** — Expected: compile FAIL.
- [ ] **Step 3: Implement** controller + catalog entries.
- [ ] **Step 4: Run** — PASS; full `.\gradlew.bat test` green (expect PermissionCatalog count tests, if any, to be updated here).
- [ ] **Step 5: Commit** — `feat: AR controller and permissions`

### Task 8: Frontend — types, API, tabs, project form

**Files (D:\Projects\maiu\erp-frontend):**
- Modify: `src/types/finance.ts`, `src/types/project.ts` (contractAmount, retentionRate on project types/forms)
- Create: `src/api/ar.api.ts`
- Create: `components/finance/invoices-tab.tsx`, `receipts-tab.tsx`, `ar-aging-card.tsx`
- Modify: `pages/finance.tsx` (Invoices / Receipts / AR Aging tabs), project form component (two new optional numeric fields)

**Interfaces:**
- Consumes: Task 7 endpoints; existing `apiRequest` (bodies must be `JSON.stringify`'d), finance-format helpers, `canAccessFinance`, bills/payments tab patterns.
- Produces types mirroring backend records: `InvoiceKind`, `InvoiceStatus`, `InvoiceLineDto`, `InvoiceDto`, `InvoiceLinePayload`, `CreateInvoicePayload`, `ReceiptAllocationDto`, `ReceiptDto`, `CreateReceiptPayload`, `ArAgingRowDto`, `InvoiceableLineDto`, `ProjectBillingSummaryDto`; API functions `getInvoices/getInvoice/createInvoice/updateInvoice/approveInvoice/cancelInvoice/releaseRetention/getInvoiceableLines/getProjectBillingSummary/createReceipt/getReceipts/getReceipt/getArAging`.
- invoices-tab: list with kind/status filters; create dialog with SO mode (SO picker → prefill from getInvoiceableLines remaining) and progress mode (project picker → show billing summary; % or peso input converting % to pesos client-side before submit; retention preview); approve/cancel; release-retention button when summary shows releasable (retentionHeld − retentionReleased > 0). receipts-tab mirrors payments-tab (open invoices, Pay-in-full). ar-aging-card mirrors ap-aging-card.

- [ ] **Step 1: Types + API** — add types and `ar.api.ts`.
- [ ] **Step 2: Components + wiring** — three components, finance page tabs, project form fields.
- [ ] **Step 3: Verify** — `npx tsc --noEmit` then `npx next build`. Expected: both clean.
- [ ] **Step 4: Commit** — `feat: AR invoices, receipts, aging UI and project contract fields`
