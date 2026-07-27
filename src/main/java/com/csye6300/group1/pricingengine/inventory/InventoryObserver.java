package com.csye6300.group1.pricingengine.inventory;

/**
 * Observer pattern: any component that needs to react to inventory changes
 * (e.g. the repricing workflow) implements this interface and registers
 * itself with the Inventory singleton.
 */
public interface InventoryObserver {
    void onStockChanged(StockChangeEvent event);
}
