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
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;

class FinancePurchaseOrderReaderTest {
    private final PurchaseOrderRepository poRepository = Mockito.mock(PurchaseOrderRepository.class);
    private final PurchaseOrderItemRepository poItemRepository = Mockito.mock(PurchaseOrderItemRepository.class);
    private final PurchaseReturnRepository returnRepository = Mockito.mock(PurchaseReturnRepository.class);
    private final PurchaseReturnItemRepository returnItemRepository = Mockito.mock(PurchaseReturnItemRepository.class);
    private final ProductRepository productRepository = Mockito.mock(ProductRepository.class);

    private final FinancePurchaseOrderReader reader = new FinancePurchaseOrderReader(
            poRepository, poItemRepository, returnRepository, returnItemRepository, productRepository);

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

    @Test
    void snapshotNamesProductsAndAllocatesReturnsAcrossDuplicateProductItems() {
        UUID poId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setPoNumber("PO-88");
        po.setSupplierId(supplierId);
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        Mockito.when(poRepository.findById(poId)).thenReturn(Optional.of(po));

        // Same product on two PO items: 10 then 4; 12 returned in total.
        PurchaseOrderItem first = poItem(poId, productA, "10.00", "20.00");
        PurchaseOrderItem second = poItem(poId, productA, "4.00", "21.00");
        Mockito.when(poItemRepository.findByPurchaseOrderId(poId)).thenReturn(List.of(first, second));

        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setId(UUID.randomUUID());
        Mockito.when(returnRepository.findByPurchaseOrderId(poId)).thenReturn(List.of(purchaseReturn));
        Mockito.when(returnItemRepository.findByPurchaseReturnId(purchaseReturn.getId()))
                .thenReturn(List.of(returnItem(productA, "12.00")));

        Product product = new Product();
        product.setId(productA);
        product.setName("Cement 40kg");
        Mockito.when(productRepository.findById(productA)).thenReturn(Optional.of(product));

        PurchaseOrderReader.PoSnapshot snapshot = reader.find(poId).orElseThrow();

        PurchaseOrderReader.PoLine lineOne = snapshot.lines().stream()
                .filter(line -> line.poItemId().equals(first.getId())).findFirst().orElseThrow();
        PurchaseOrderReader.PoLine lineTwo = snapshot.lines().stream()
                .filter(line -> line.poItemId().equals(second.getId())).findFirst().orElseThrow();

        assertEquals("Cement 40kg", lineOne.productName());
        // Returned 12 fills the first item's 10, then 2 against the second.
        assertEquals(new BigDecimal("10.00"), lineOne.returnedQuantity());
        assertEquals(new BigDecimal("2.00"), lineTwo.returnedQuantity());
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
