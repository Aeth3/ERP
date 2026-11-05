package com.maiu.erp.modules.finance.infrastructure.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.finance.domain.model.Payment;
import com.maiu.erp.modules.finance.domain.model.PaymentAllocation;
import com.maiu.erp.modules.finance.domain.repository.PaymentRepository;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.PaymentAllocationEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.PaymentEntity;
import com.maiu.erp.modules.finance.infrastructure.persistence.jpa.PaymentJpaRepository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryImpl(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return toDomain(jpaRepository.save(toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Payment> findBySupplierId(UUID supplierId) {
        return jpaRepository.findBySupplierId(supplierId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Payment> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private Payment toDomain(PaymentEntity entity) {
        Payment payment = new Payment();
        payment.setId(entity.getId());
        payment.setPaymentNumber(entity.getPaymentNumber());
        payment.setSupplierId(entity.getSupplierId());
        payment.setPaymentDate(entity.getPaymentDate());
        payment.setMethod(entity.getMethod());
        payment.setReference(entity.getReference());
        payment.setTotal(entity.getTotal());
        payment.setJournalEntryId(entity.getJournalEntryId());
        payment.setCreatedBy(entity.getCreatedBy());
        payment.setCreatedAt(entity.getCreatedAt());
        List<PaymentAllocation> allocations = new ArrayList<>();
        for (PaymentAllocationEntity allocationEntity : entity.getAllocations()) {
            PaymentAllocation allocation = new PaymentAllocation();
            allocation.setId(allocationEntity.getId());
            allocation.setBillId(allocationEntity.getBillId());
            allocation.setAmount(allocationEntity.getAmount());
            allocations.add(allocation);
        }
        payment.setAllocations(allocations);
        return payment;
    }

    private PaymentEntity toEntity(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(payment.getId());
        entity.setPaymentNumber(payment.getPaymentNumber());
        entity.setSupplierId(payment.getSupplierId());
        entity.setPaymentDate(payment.getPaymentDate());
        entity.setMethod(payment.getMethod());
        entity.setReference(payment.getReference());
        entity.setTotal(payment.getTotal());
        entity.setJournalEntryId(payment.getJournalEntryId());
        entity.setCreatedBy(payment.getCreatedBy());
        entity.setCreatedAt(payment.getCreatedAt());
        List<PaymentAllocationEntity> allocationEntities = new ArrayList<>();
        for (PaymentAllocation allocation : payment.getAllocations()) {
            PaymentAllocationEntity allocationEntity = new PaymentAllocationEntity();
            allocationEntity.setId(allocation.getId());
            allocationEntity.setBillId(allocation.getBillId());
            allocationEntity.setAmount(allocation.getAmount());
            allocationEntities.add(allocationEntity);
        }
        entity.setAllocations(allocationEntities);
        return entity;
    }
}
