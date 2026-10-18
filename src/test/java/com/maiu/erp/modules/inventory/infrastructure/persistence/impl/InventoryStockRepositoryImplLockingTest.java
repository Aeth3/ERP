package com.maiu.erp.modules.inventory.infrastructure.persistence.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.maiu.erp.modules.inventory.domain.model.InventoryStock;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(InventoryStockRepositoryImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InventoryStockRepositoryImplLockingTest {

    @Autowired
    private InventoryStockRepositoryImpl inventoryStockRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentReadCheckWriteUpdatesDoNotLoseStock() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            InventoryStock stock = new InventoryStock();
            stock.setProductId(productId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantityOnHand(BigDecimal.ZERO);
            stock.setReservedQuantity(BigDecimal.ZERO);
            stock.setReorderLevel(BigDecimal.ZERO);
            stock.setUpdatedAt(Instant.now());
            inventoryStockRepository.save(stock);
        });

        CountDownLatch firstHasRead = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // The first writer reads, then holds its transaction open before writing. Without a
            // row lock the second writer reads the same starting quantity and one update is lost.
            Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                InventoryStock stock = inventoryStockRepository
                        .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                        .orElseThrow();
                firstHasRead.countDown();
                sleep(300);
                stock.setQuantityOnHand(stock.getQuantityOnHand().add(new BigDecimal("5")));
                inventoryStockRepository.save(stock);
            }));
            Future<?> second = executor.submit(() -> {
                await(firstHasRead);
                transaction.executeWithoutResult(status -> {
                    InventoryStock stock = inventoryStockRepository
                            .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                            .orElseThrow();
                    stock.setQuantityOnHand(stock.getQuantityOnHand().add(new BigDecimal("5")));
                    inventoryStockRepository.save(stock);
                });
            });

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        BigDecimal finalQuantity = inventoryStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow()
                .getQuantityOnHand();
        assertEquals(0, new BigDecimal("10").compareTo(finalQuantity), "expected 10 but was " + finalQuantity);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("First writer never read the stock row");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
