package com.maiu.erp.modules.inventory.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.maiu.erp.modules.finance.application.port.SalesOrderReader;
import com.maiu.erp.modules.inventory.domain.enums.SalesOrderStatus;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.model.SalesReturnItem;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnRepository;

class FinanceSalesOrderReaderTest {
    private final SalesOrderRepository soRepository = Mockito.mock(SalesOrderRepository.class);
    private final SalesOrderItemRepository soItemRepository = Mockito.mock(SalesOrderItemRepository.class);
    private final SalesReturnRepository returnRepository = Mockito.mock(SalesReturnRepository.class);
    private final SalesReturnItemRepository returnItemRepository = Mockito.mock(SalesReturnItemRepository.class);
    private final ProductRepository productRepository = Mockito.mock(ProductRepository.class);

    private final FinanceSalesOrderReader reader = new FinanceSalesOrderReader(
            soRepository, soItemRepository, returnRepository, returnItemRepository, productRepository);

    @Test
    void emptyWhenSalesOrderMissing() {
        UUID soId = UUID.randomUUID();
        Mockito.when(soRepository.findById(soId)).thenReturn(Optional.empty());

        assertTrue(reader.find(soId).isEmpty());
    }

    @Test
    void mapsSnapshotFields() {
        UUID soId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(soId);
        salesOrder.setSoNumber("SO-42");
        salesOrder.setCustomerId(customerId);
        salesOrder.setStatus(SalesOrderStatus.SHIPPED);
        Mockito.when(soRepository.findById(soId)).thenReturn(Optional.of(salesOrder));

        SalesOrderItem item = soItem(soId, productId, "10.00", "25.00");
        Mockito.when(soItemRepository.findBySalesOrderId(soId)).thenReturn(List.of(item));
        Mockito.when(returnRepository.findBySalesOrderId(soId)).thenReturn(List.of());

        Product product = new Product();
        product.setId(productId);
        product.setName("Rebar 10mm");
        Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        SalesOrderReader.SoSnapshot snapshot = reader.find(soId).orElseThrow();

        assertEquals("SO-42", snapshot.soNumber());
        assertEquals(customerId, snapshot.customerId());
        assertEquals("SHIPPED", snapshot.status());
        assertEquals(1, snapshot.lines().size());
        SalesOrderReader.SoLine line = snapshot.lines().get(0);
        assertEquals(item.getId(), line.soItemId());
        assertEquals(productId, line.productId());
        assertEquals("Rebar 10mm", line.productName());
        assertEquals(new BigDecimal("10.00"), line.quantity());
        assertEquals(BigDecimal.ZERO, line.returnedQuantity());
        assertEquals(new BigDecimal("25.00"), line.unitPrice());
    }

    @Test
    void allocatesReturnsGreedilyAcrossDuplicateProductItems() {
        UUID soId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(soId);
        salesOrder.setSoNumber("SO-43");
        salesOrder.setCustomerId(UUID.randomUUID());
        salesOrder.setStatus(SalesOrderStatus.DELIVERED);
        Mockito.when(soRepository.findById(soId)).thenReturn(Optional.of(salesOrder));

        // Same product on two SO items: 5 then 5; 7 returned in total.
        SalesOrderItem first = soItem(soId, productId, "5.00", "25.00");
        SalesOrderItem second = soItem(soId, productId, "5.00", "26.00");
        Mockito.when(soItemRepository.findBySalesOrderId(soId)).thenReturn(List.of(first, second));

        SalesReturn salesReturn = new SalesReturn();
        salesReturn.setId(UUID.randomUUID());
        Mockito.when(returnRepository.findBySalesOrderId(soId)).thenReturn(List.of(salesReturn));
        SalesReturnItem returnItem = new SalesReturnItem();
        returnItem.setProductId(productId);
        returnItem.setQuantity(new BigDecimal("7.00"));
        Mockito.when(returnItemRepository.findBySalesReturnId(salesReturn.getId()))
                .thenReturn(List.of(returnItem));

        SalesOrderReader.SoSnapshot snapshot = reader.find(soId).orElseThrow();

        SalesOrderReader.SoLine lineOne = snapshot.lines().stream()
                .filter(line -> line.soItemId().equals(first.getId())).findFirst().orElseThrow();
        SalesOrderReader.SoLine lineTwo = snapshot.lines().stream()
                .filter(line -> line.soItemId().equals(second.getId())).findFirst().orElseThrow();

        assertEquals(new BigDecimal("5.00"), lineOne.returnedQuantity());
        assertEquals(new BigDecimal("2.00"), lineTwo.returnedQuantity());
    }

    private SalesOrderItem soItem(UUID soId, UUID productId, String quantity, String unitPrice) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(UUID.randomUUID());
        item.setSalesOrderId(soId);
        item.setProductId(productId);
        item.setQuantity(new BigDecimal(quantity));
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }
}
