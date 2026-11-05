package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.domain.model.Bill;
import com.maiu.erp.modules.finance.domain.model.BillLine;
import com.maiu.erp.modules.finance.domain.repository.BillRepository;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.BillEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.BillLineEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.jpa.BillJpaRepository;

@Repository
public class BillRepositoryImpl implements BillRepository {
    private final BillJpaRepository jpaRepository;

    public BillRepositoryImpl(BillJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Bill save(Bill bill) {
        return toDomain(jpaRepository.save(toEntity(bill)));
    }

    @Override
    public Optional<Bill> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Bill> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public List<Bill> findByPurchaseOrderId(UUID purchaseOrderId) {
        return jpaRepository.findByPurchaseOrderId(purchaseOrderId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Bill> search(UUID supplierId, BillStatus status, UUID purchaseOrderId) {
        return jpaRepository.search(supplierId, status, purchaseOrderId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsBySupplierIdAndBillNumberIgnoreCaseAndStatusNot(UUID supplierId, String billNumber,
            BillStatus excluded) {
        return jpaRepository.existsBySupplierIdAndBillNumberIgnoreCaseAndStatusNot(supplierId, billNumber, excluded);
    }

    @Override
    public List<Bill> findOpen() {
        return jpaRepository.findByStatusIn(List.of(BillStatus.APPROVED, BillStatus.PARTIALLY_PAID)).stream()
                .map(this::toDomain)
                .toList();
    }

    private Bill toDomain(BillEntity entity) {
        Bill bill = new Bill();
        bill.setId(entity.getId());
        bill.setBillNumber(entity.getBillNumber());
        bill.setSupplierId(entity.getSupplierId());
        bill.setPurchaseOrderId(entity.getPurchaseOrderId());
        bill.setBillDate(entity.getBillDate());
        bill.setDueDate(entity.getDueDate());
        bill.setStatus(entity.getStatus());
        bill.setSubtotal(entity.getSubtotal());
        bill.setTotal(entity.getTotal());
        bill.setAmountPaid(entity.getAmountPaid());
        bill.setJournalEntryId(entity.getJournalEntryId());
        bill.setNotes(entity.getNotes());
        bill.setCreatedBy(entity.getCreatedBy());
        bill.setCreatedAt(entity.getCreatedAt());
        List<BillLine> lines = new ArrayList<>();
        for (BillLineEntity lineEntity : entity.getLines()) {
            BillLine line = new BillLine();
            line.setId(lineEntity.getId());
            line.setLineNumber(lineEntity.getLineNumber());
            line.setProductId(lineEntity.getProductId());
            line.setPurchaseOrderItemId(lineEntity.getPurchaseOrderItemId());
            line.setQuantity(lineEntity.getQuantity());
            line.setUnitCost(lineEntity.getUnitCost());
            line.setAccountId(lineEntity.getAccountId());
            line.setDescription(lineEntity.getDescription());
            line.setLineTotal(lineEntity.getLineTotal());
            lines.add(line);
        }
        bill.setLines(lines);
        return bill;
    }

    private BillEntity toEntity(Bill bill) {
        BillEntity entity = new BillEntity();
        entity.setId(bill.getId());
        entity.setBillNumber(bill.getBillNumber());
        entity.setSupplierId(bill.getSupplierId());
        entity.setPurchaseOrderId(bill.getPurchaseOrderId());
        entity.setBillDate(bill.getBillDate());
        entity.setDueDate(bill.getDueDate());
        entity.setStatus(bill.getStatus());
        entity.setSubtotal(bill.getSubtotal());
        entity.setTotal(bill.getTotal());
        entity.setAmountPaid(bill.getAmountPaid());
        entity.setJournalEntryId(bill.getJournalEntryId());
        entity.setNotes(bill.getNotes());
        entity.setCreatedBy(bill.getCreatedBy());
        entity.setCreatedAt(bill.getCreatedAt());
        List<BillLineEntity> lineEntities = new ArrayList<>();
        for (BillLine line : bill.getLines()) {
            BillLineEntity lineEntity = new BillLineEntity();
            lineEntity.setId(line.getId());
            lineEntity.setLineNumber(line.getLineNumber());
            lineEntity.setProductId(line.getProductId());
            lineEntity.setPurchaseOrderItemId(line.getPurchaseOrderItemId());
            lineEntity.setQuantity(line.getQuantity());
            lineEntity.setUnitCost(line.getUnitCost());
            lineEntity.setAccountId(line.getAccountId());
            lineEntity.setDescription(line.getDescription());
            lineEntity.setLineTotal(line.getLineTotal());
            lineEntities.add(lineEntity);
        }
        entity.setLines(lineEntities);
        return entity;
    }
}
