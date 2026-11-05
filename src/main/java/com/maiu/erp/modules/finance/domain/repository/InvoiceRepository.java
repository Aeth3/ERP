package com.maiu.erp.modules.finance.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.model.Invoice;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    /** Locks the invoice row for the transaction; use for approval, cancellation and receipts. */
    Optional<Invoice> findByIdForUpdate(UUID id);

    List<Invoice> findBySalesOrderId(UUID salesOrderId);

    List<Invoice> findByProjectId(UUID projectId);

    /** All filters optional; null means "any". */
    List<Invoice> search(UUID customerId, InvoiceStatus status, InvoiceKind kind);

    boolean existsByInvoiceNumber(String invoiceNumber);

    /** Invoices with an outstanding balance: APPROVED or PARTIALLY_PAID. */
    List<Invoice> findOpen();
}
