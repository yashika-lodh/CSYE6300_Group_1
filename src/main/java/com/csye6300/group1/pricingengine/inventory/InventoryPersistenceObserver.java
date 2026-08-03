package com.csye6300.group1.pricingengine.inventory;

/**
 * Observer pattern: bridges the in-memory Inventory singleton to durable
 * storage. Registered the same way RepricingTriggerObserver is registered
 * for repricing - every stock change is written through to the database so
 * it survives a restart, without Inventory ever needing to know JPA exists.
 */
public class InventoryPersistenceObserver implements InventoryObserver {

    private final InventoryRepository repository;

    public InventoryPersistenceObserver(InventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onStockChanged(StockChangeEvent event) {
        repository.upsertStock(event.getSku(), event.getNewQuantity());
    }
}
