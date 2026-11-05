package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.domain.model.Invoice;
import com.maiu.erp.modules.finance.domain.model.InvoiceLine;
import com.maiu.erp.modules.finance.domain.repository.InvoiceRepository;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.InvoiceEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.InvoiceLineEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.jpa.InvoiceJpaRepository;

@Repository
public class InvoiceRepositoryImpl implements InvoiceRepository {
    private final InvoiceJpaRepository jpaRepository;

    public InvoiceRepositoryImpl(InvoiceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Invoice save(Invoice invoice) {
        return toDomain(jpaRepository.save(toEntity(invoice)));
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public List<Invoice> findBySalesOrderId(UUID salesOrderId) {
        return jpaRepository.findBySalesOrderId(salesOrderId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Invoice> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Invoice> search(UUID customerId, InvoiceStatus status, InvoiceKind kind) {
        return jpaRepository.search(customerId, status, kind).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByInvoiceNumber(String invoiceNumber) {
        return jpaRepository.existsByInvoiceNumber(invoiceNumber);
    }

    @Override
    public List<Invoice> findOpen() {
        return jpaRepository.findByStatusIn(List.of(InvoiceStatus.APPROVED, InvoiceStatus.PARTIALLY_PAID)).stream()
                .map(this::toDomain)
                .toList();
    }

    private Invoice toDomain(InvoiceEntity entity) {
        Invoice invoice = new Invoice();
        invoice.setId(entity.getId());
        invoice.setInvoiceNumber(entity.getInvoiceNumber());
        invoice.setKind(entity.getKind());
        invoice.setCustomerId(entity.getCustomerId());
        invoice.setSalesOrderId(entity.getSalesOrderId());
        invoice.setProjectId(entity.getProjectId());
        invoice.setInvoiceDate(entity.getInvoiceDate());
        invoice.setDueDate(entity.getDueDate());
        invoice.setStatus(entity.getStatus());
        invoice.setSubtotal(entity.getSubtotal());
        invoice.setRetentionAmount(entity.getRetentionAmount());
        invoice.setTotal(entity.getTotal());
        invoice.setAmountReceived(entity.getAmountReceived());
        invoice.setJournalEntryId(entity.getJournalEntryId());
        invoice.setNotes(entity.getNotes());
        invoice.setCreatedBy(entity.getCreatedBy());
        invoice.setCreatedAt(entity.getCreatedAt());
        List<InvoiceLine> lines = new ArrayList<>();
        for (InvoiceLineEntity lineEntity : entity.getLines()) {
            InvoiceLine line = new InvoiceLine();
            line.setId(lineEntity.getId());
            line.setLineNumber(lineEntity.getLineNumber());
            line.setSalesOrderItemId(lineEntity.getSalesOrderItemId());
            line.setProductId(lineEntity.getProductId());
            line.setDescription(lineEntity.getDescription());
            line.setQuantity(lineEntity.getQuantity());
            line.setUnitPrice(lineEntity.getUnitPrice());
            line.setLineTotal(lineEntity.getLineTotal());
            lines.add(line);
        }
        invoice.setLines(lines);
        return invoice;
    }

    private InvoiceEntity toEntity(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(invoice.getId());
        entity.setInvoiceNumber(invoice.getInvoiceNumber());
        entity.setKind(invoice.getKind());
        entity.setCustomerId(invoice.getCustomerId());
        entity.setSalesOrderId(invoice.getSalesOrderId());
        entity.setProjectId(invoice.getProjectId());
        entity.setInvoiceDate(invoice.getInvoiceDate());
        entity.setDueDate(invoice.getDueDate());
        entity.setStatus(invoice.getStatus());
        entity.setSubtotal(invoice.getSubtotal());
        entity.setRetentionAmount(invoice.getRetentionAmount());
        entity.setTotal(invoice.getTotal());
        entity.setAmountReceived(invoice.getAmountReceived());
        entity.setJournalEntryId(invoice.getJournalEntryId());
        entity.setNotes(invoice.getNotes());
        entity.setCreatedBy(invoice.getCreatedBy());
        entity.setCreatedAt(invoice.getCreatedAt());
        List<InvoiceLineEntity> lineEntities = new ArrayList<>();
        for (InvoiceLine line : invoice.getLines()) {
            InvoiceLineEntity lineEntity = new InvoiceLineEntity();
            lineEntity.setId(line.getId());
            lineEntity.setLineNumber(line.getLineNumber());
            lineEntity.setSalesOrderItemId(line.getSalesOrderItemId());
            lineEntity.setProductId(line.getProductId());
            lineEntity.setDescription(line.getDescription());
            lineEntity.setQuantity(line.getQuantity());
            lineEntity.setUnitPrice(line.getUnitPrice());
            lineEntity.setLineTotal(line.getLineTotal());
            lineEntities.add(lineEntity);
        }
        entity.setLines(lineEntities);
        return entity;
    }
}
