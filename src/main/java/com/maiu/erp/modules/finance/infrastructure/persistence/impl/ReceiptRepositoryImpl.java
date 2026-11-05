package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.finance.domain.model.Receipt;
import com.maiu.erp.modules.finance.domain.model.ReceiptAllocation;
import com.maiu.erp.modules.finance.domain.repository.ReceiptRepository;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.ReceiptAllocationEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.ReceiptEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.jpa.ReceiptJpaRepository;

@Repository
public class ReceiptRepositoryImpl implements ReceiptRepository {
    private final ReceiptJpaRepository jpaRepository;

    public ReceiptRepositoryImpl(ReceiptJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Receipt save(Receipt receipt) {
        return toDomain(jpaRepository.save(toEntity(receipt)));
    }

    @Override
    public Optional<Receipt> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Receipt> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerId(customerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Receipt> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByReceiptNumber(String receiptNumber) {
        return jpaRepository.existsByReceiptNumber(receiptNumber);
    }

    private Receipt toDomain(ReceiptEntity entity) {
        Receipt receipt = new Receipt();
        receipt.setId(entity.getId());
        receipt.setReceiptNumber(entity.getReceiptNumber());
        receipt.setCustomerId(entity.getCustomerId());
        receipt.setReceiptDate(entity.getReceiptDate());
        receipt.setMethod(entity.getMethod());
        receipt.setReference(entity.getReference());
        receipt.setTotal(entity.getTotal());
        receipt.setJournalEntryId(entity.getJournalEntryId());
        receipt.setCreatedBy(entity.getCreatedBy());
        receipt.setCreatedAt(entity.getCreatedAt());
        List<ReceiptAllocation> allocations = new ArrayList<>();
        for (ReceiptAllocationEntity allocationEntity : entity.getAllocations()) {
            ReceiptAllocation allocation = new ReceiptAllocation();
            allocation.setId(allocationEntity.getId());
            allocation.setInvoiceId(allocationEntity.getInvoiceId());
            allocation.setAmount(allocationEntity.getAmount());
            allocations.add(allocation);
        }
        receipt.setAllocations(allocations);
        return receipt;
    }

    private ReceiptEntity toEntity(Receipt receipt) {
        ReceiptEntity entity = new ReceiptEntity();
        entity.setId(receipt.getId());
        entity.setReceiptNumber(receipt.getReceiptNumber());
        entity.setCustomerId(receipt.getCustomerId());
        entity.setReceiptDate(receipt.getReceiptDate());
        entity.setMethod(receipt.getMethod());
        entity.setReference(receipt.getReference());
        entity.setTotal(receipt.getTotal());
        entity.setJournalEntryId(receipt.getJournalEntryId());
        entity.setCreatedBy(receipt.getCreatedBy());
        entity.setCreatedAt(receipt.getCreatedAt());
        List<ReceiptAllocationEntity> allocationEntities = new ArrayList<>();
        for (ReceiptAllocation allocation : receipt.getAllocations()) {
            ReceiptAllocationEntity allocationEntity = new ReceiptAllocationEntity();
            allocationEntity.setId(allocation.getId());
            allocationEntity.setInvoiceId(allocation.getInvoiceId());
            allocationEntity.setAmount(allocation.getAmount());
            allocationEntities.add(allocationEntity);
        }
        entity.setAllocations(allocationEntities);
        return entity;
    }
}
