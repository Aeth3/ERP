package com.maiu.erp.modules.inventory.application.port;

/**
 * Posts valued stock changes to the general ledger. Called inside the stock transaction,
 * so a posting failure rolls back the stock change too.
 */
public interface InventoryLedgerPoster {
    void post(InventoryLedgerEvent event);
}
