package com.csye6300.group1.pricingengine.inventory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Singleton pattern: there is exactly one Inventory shared by every SalesChannel
 * so that stock levels stay consistent no matter which channel updates them.
 *
 * Thread-safety: double-checked locking on getInstance(), ConcurrentHashMap for
 * stock, and CopyOnWriteArrayList for observers so notifications are safe even
 * if a repricing workflow is running on another thread.
 *
 * Observer pattern: Inventory is the "subject" - it notifies every registered
 * InventoryObserver (e.g. the repricing workflow) whenever stock changes.
 */
public final class Inventory {

    private static volatile Inventory instance;

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();
    private final List<InventoryObserver> observers = new CopyOnWriteArrayList<>();
    private final Logger logger = Logger.getLogger(Inventory.class.getName());

    private Inventory() {
        // private constructor enforces Singleton
    }

    public static Inventory getInstance() {
        Inventory result = instance;
        if (result == null) {
            synchronized (Inventory.class) {
                result = instance;
                if (result == null) {
                    instance = result = new Inventory();
                }
            }
        }
        return result;
    }

    public void addObserver(InventoryObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(InventoryObserver observer) {
        observers.remove(observer);
    }

    /** Sets the absolute stock level for a SKU and notifies observers of the change. */
    public void updateStock(String sku, int newQuantity) {
        int previous = stock.getOrDefault(sku, 0);
        stock.put(sku, newQuantity);
        logger.fine(() -> "Stock updated for " + sku + ": " + previous + " -> " + newQuantity);
        notifyObservers(new StockChangeEvent(sku, previous, newQuantity));
    }

    /** Applies a relative delta (positive = restock, negative = sale/decrement). */
    public void adjustStock(String sku, int delta) {
        updateStock(sku, getStock(sku) + delta);
    }

    public int getStock(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    public Map<String, Integer> snapshot() {
        return Map.copyOf(stock);
    }

    /**
     * Seeds the in-memory stock map from durable storage at startup
     * (Milestone 3: InventoryRepository, replacing the Milestone 2 CSV load).
     * Does not notify observers - this is a silent load, not a stock change.
     */
    public void loadFromRepository(InventoryRepository repository) {
        stock.putAll(repository.findAll());
    }

    private void notifyObservers(StockChangeEvent event) {
        for (InventoryObserver observer : observers) {
            observer.onStockChanged(event);
        }
    }

    /** Test-only hook so unit tests can reset shared singleton state between tests. */
    public void resetForTesting() {
        stock.clear();
        observers.clear();
    }
}
