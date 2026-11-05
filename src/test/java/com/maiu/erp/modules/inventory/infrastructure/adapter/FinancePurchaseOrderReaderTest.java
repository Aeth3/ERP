package com.maiu.erp.modules.inventory.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.maiu.erp.modules.finance.application.port.PurchaseOrderReader;
import com.maiu.erp.modules.inventory.domain.enums.PurchaseOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;

class FinancePurchaseOrderReaderTest {
    private final PurchaseOrderRepository poRepository = Mockito.mock(PurchaseOrderRepository.class);
    private final PurchaseOrderItemRepository poItemRepository = Mockito.mock(PurchaseOrderItemRepository.class);
    private final PurchaseReturnRepository returnRepository = Mockito.mock(PurchaseReturnRepository.class);
    private final PurchaseReturnItemRepository returnItemRepository = Mockito.mock(PurchaseReturnItemRepository.class);

    private final FinancePurchaseOrderReader reader = new FinancePurchaseOrderReader(
            poRepository, poItemRepository, returnRepository, returnItemRepository);

    @Test
    void returnsEmptyForUnknownPurchaseOrder() {
        UUID poId = UUID.randomUUID();
        Mockito.when(poRepository.findById(poId)).thenReturn(Optional.empty());

        assertTrue(reader.find(poId).isEmpty());
    }

    @Test
    void snapshotSumsReturnedQuantityPerProductAcrossReturns() {
        UUID poId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setPoNumber("PO-77");
        po.setSupplierId(supplierId);
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        Mockito.when(poRepository.findById(poId)).thenReturn(Optional.of(po));

        PurchaseOrderItem itemA = poItem(poId, productA, "10.00", "20.00");
        PurchaseOrderItem itemB = poItem(poId, productB, "4.00", "50.00");
        Mockito.when(poItemRepository.findByPurchaseOrderId(poId)).thenReturn(List.of(itemA, itemB));

        PurchaseReturn firstReturn = new PurchaseReturn();
        firstReturn.setId(UUID.randomUUID());
        PurchaseReturn secondReturn = new PurchaseReturn();
        secondReturn.setId(UUID.randomUUID());
        Mockito.when(returnRepository.findByPurchaseOrderId(poId)).thenReturn(List.of(firstReturn, secondReturn));
        Mockito.when(returnItemRepository.findByPurchaseReturnId(firstReturn.getId()))
                .thenReturn(List.of(returnItem(productA, "2.00")));
        Mockito.when(returnItemRepository.findByPurchaseReturnId(secondReturn.getId()))
                .thenReturn(List.of(returnItem(productA, "1.00")));

        PurchaseOrderReader.PoSnapshot snapshot = reader.find(poId).orElseThrow();

        assertEquals("PO-77", snapshot.poNumber());
        assertEquals(supplierId, snapshot.supplierId());
        assertEquals("RECEIVED", snapshot.status());
        assertEquals(2, snapshot.lines().size());

        PurchaseOrderReader.PoLine lineA = snapshot.lines().stream()
                .filter(line -> line.productId().equals(productA)).findFirst().orElseThrow();
        assertEquals(itemA.getId(), lineA.poItemId());
        assertEquals(new BigDecimal("10.00"), lineA.quantity());
        assertEquals(new BigDecimal("3.00"), lineA.returnedQuantity());
        assertEquals(new BigDecimal("20.00"), lineA.unitCost());

        PurchaseOrderReader.PoLine lineB = snapshot.lines().stream()
                .filter(line -> line.productId().equals(productB)).findFirst().orElseThrow();
        assertEquals(BigDecimal.ZERO, lineB.returnedQuantity());
    }

    private PurchaseOrderItem poItem(UUID poId, UUID productId, String quantity, String unitCost) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(UUID.randomUUID());
        item.setPurchaseOrderId(poId);
        item.setProductId(productId);
        item.setQuantity(new BigDecimal(quantity));
        item.setUnitCost(new BigDecimal(unitCost));
        return item;
    }

    private PurchaseReturnItem returnItem(UUID productId, String quantity) {
        PurchaseReturnItem item = new PurchaseReturnItem();
        item.setId(UUID.randomUUID());
        item.setProductId(productId);
        item.setQuantity(new BigDecimal(quantity));
        return item;
    }
}
