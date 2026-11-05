package com.maiu.erp.modules.finance.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.domain.repository.InvoiceRepository;

public final class InMemoryInvoiceRepository implements InvoiceRepository {
    private final List<Invoice> invoices = new ArrayList<>();
    private int forUpdateCalls;

    public int getForUpdateCalls() {
        return forUpdateCalls;
    }

    @Override
    public Invoice save(Invoice invoice) {
        if (invoice.getId() == null) {
            invoice.setId(UUID.randomUUID());
            invoices.add(invoice);
        } else if (invoices.stream().noneMatch(existing -> invoice.getId().equals(existing.getId()))) {
            invoices.add(invoice);
        } else {
            invoices.replaceAll(existing -> invoice.getId().equals(existing.getId()) ? invoice : existing);
        }
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return invoices.stream().filter(invoice -> id.equals(invoice.getId())).findFirst();
    }

    @Override
    public Optional<Invoice> findByIdForUpdate(UUID id) {
        forUpdateCalls++;
        return findById(id);
    }

    @Override
    public List<Invoice> findBySalesOrderId(UUID salesOrderId) {
        return invoices.stream().filter(invoice -> salesOrderId.equals(invoice.getSalesOrderId())).toList();
    }

    @Override
    public List<Invoice> findByProjectId(UUID projectId) {
        return invoices.stream().filter(invoice -> projectId.equals(invoice.getProjectId())).toList();
    }

    @Override
    public List<Invoice> search(UUID customerId, InvoiceStatus status, InvoiceKind kind) {
        return invoices.stream()
                .filter(invoice -> customerId == null || customerId.equals(invoice.getCustomerId()))
                .filter(invoice -> status == null || status == invoice.getStatus())
                .filter(invoice -> kind == null || kind == invoice.getKind())
                .toList();
    }

    @Override
    public boolean existsByInvoiceNumber(String invoiceNumber) {
        return invoices.stream().anyMatch(invoice -> invoiceNumber.equals(invoice.getInvoiceNumber()));
    }

    @Override
    public List<Invoice> findOpen() {
        return invoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.APPROVED
                        || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .toList();
    }
}
