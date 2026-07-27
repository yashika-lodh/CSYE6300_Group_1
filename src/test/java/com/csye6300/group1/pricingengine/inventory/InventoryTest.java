package com.csye6300.group1.pricingengine.inventory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    @AfterEach
    void tearDown() {
        Inventory.getInstance().resetForTesting();
    }

    @Test
    void getInstanceReturnsSameObjectEveryTime() {
        Inventory first = Inventory.getInstance();
        Inventory second = Inventory.getInstance();
        assertSame(first, second, "Inventory must be a single shared instance (Singleton)");
    }

    @Test
    void updateStockStoresQuantity() {
        Inventory inventory = Inventory.getInstance();
        inventory.updateStock("SKU-1", 25);
        assertEquals(25, inventory.getStock("SKU-1"));
    }

    @Test
    void adjustStockAppliesRelativeDelta() {
        Inventory inventory = Inventory.getInstance();
        inventory.updateStock("SKU-1", 10);
        inventory.adjustStock("SKU-1", -3);
        assertEquals(7, inventory.getStock("SKU-1"));
    }

    @Test
    void observersAreNotifiedOnStockChange() {
        Inventory inventory = Inventory.getInstance();
        List<StockChangeEvent> received = new ArrayList<>();
        InventoryObserver observer = received::add;

        inventory.addObserver(observer);
        inventory.updateStock("SKU-2", 5);

        assertEquals(1, received.size());
        assertEquals("SKU-2", received.get(0).getSku());
        assertEquals(5, received.get(0).getNewQuantity());
    }

    @Test
    void removedObserversAreNotNotified() {
        Inventory inventory = Inventory.getInstance();
        List<StockChangeEvent> received = new ArrayList<>();
        InventoryObserver observer = received::add;

        inventory.addObserver(observer);
        inventory.removeObserver(observer);
        inventory.updateStock("SKU-3", 1);

        assertTrue(received.isEmpty());
    }
}
