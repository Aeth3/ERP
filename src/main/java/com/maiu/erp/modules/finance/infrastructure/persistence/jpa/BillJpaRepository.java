package com.maiu.erp.modules.finance.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maiu.erp.modules.finance.domain.enums.BillStatus;
import com.maiu.erp.modules.finance.infrastructure.persistence.entity.BillEntity;

import jakarta.persistence.LockModeType;

public interface BillJpaRepository extends JpaRepository<BillEntity, UUID> {
    List<BillEntity> findByPurchaseOrderId(UUID purchaseOrderId);

    boolean existsBySupplierIdAndBillNumberIgnoreCaseAndStatusNot(UUID supplierId, String billNumber,
            BillStatus excluded);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BillEntity b where b.id = :id")
    Optional<BillEntity> findByIdForUpdate(@Param("id") UUID id);

    List<BillEntity> findByStatusIn(List<BillStatus> statuses);

    @Query("select b from BillEntity b where (:supplierId is null or b.supplierId = :supplierId)"
            + " and (:status is null or b.status = :status)"
            + " and (:purchaseOrderId is null or b.purchaseOrderId = :purchaseOrderId)"
            + " order by b.billDate desc, b.createdAt desc")
    List<BillEntity> search(
            @Param("supplierId") UUID supplierId,
            @Param("status") BillStatus status,
            @Param("purchaseOrderId") UUID purchaseOrderId);
}
