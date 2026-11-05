package com.maiu.erp.modules.finance.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maiu.erp.modules.finance.domain.enums.InvoiceKind;
import com.maiu.erp.modules.finance.domain.enums.InvoiceStatus;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.InvoiceEntity;

import jakarta.persistence.LockModeType;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceEntity, UUID> {
    List<InvoiceEntity> findBySalesOrderId(UUID salesOrderId);

    List<InvoiceEntity> findByProjectId(UUID projectId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvoiceEntity i where i.id = :id")
    Optional<InvoiceEntity> findByIdForUpdate(@Param("id") UUID id);

    List<InvoiceEntity> findByStatusIn(List<InvoiceStatus> statuses);

    @Query("select i from InvoiceEntity i where (:customerId is null or i.customerId = :customerId)"
            + " and (:status is null or i.status = :status)"
            + " and (:kind is null or i.kind = :kind)"
            + " order by i.invoiceDate desc, i.createdAt desc")
    List<InvoiceEntity> search(
            @Param("customerId") UUID customerId,
            @Param("status") InvoiceStatus status,
            @Param("kind") InvoiceKind kind);
}
