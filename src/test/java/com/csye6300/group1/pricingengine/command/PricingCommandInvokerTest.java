package com.csye6300.group1.pricingengine.command;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingCommandInvokerTest {

    private static class InMemoryPriceReceiver implements PriceReceiver {
        private final Map<String, Double> prices = new HashMap<>();

        @Override
        public void setPrice(String sku, double price) {
            prices.put(sku, price);
        }

        @Override
        public double getPrice(String sku) {
            return prices.getOrDefault(sku, 0.0);
        }
    }

    @Test
    void executeCommandAppliesNewPrice() {
        InMemoryPriceReceiver receiver = new InMemoryPriceReceiver();
        receiver.setPrice("SKU-1", 10.0);
        PricingCommandInvoker invoker = new PricingCommandInvoker();

        invoker.executeCommand(new UpdatePriceCommand(receiver, "SKU-1", 15.0));

        assertEquals(15.0, receiver.getPrice("SKU-1"));
    }

    @Test
    void undoRestoresOriginalPrice() {
        InMemoryPriceReceiver receiver = new InMemoryPriceReceiver();
        receiver.setPrice("SKU-1", 10.0);
        PricingCommandInvoker invoker = new PricingCommandInvoker();

        invoker.executeCommand(new UpdatePriceCommand(receiver, "SKU-1", 15.0));
        boolean undone = invoker.undo();

        assertTrue(undone);
        assertEquals(10.0, receiver.getPrice("SKU-1"));
    }

    @Test
    void redoReappliesUndoneCommand() {
        InMemoryPriceReceiver receiver = new InMemoryPriceReceiver();
        receiver.setPrice("SKU-1", 10.0);
        PricingCommandInvoker invoker = new PricingCommandInvoker();

        invoker.executeCommand(new UpdatePriceCommand(receiver, "SKU-1", 15.0));
        invoker.undo();
        boolean redone = invoker.redo();

        assertTrue(redone);
        assertEquals(15.0, receiver.getPrice("SKU-1"));
    }

    @Test
    void undoOnEmptyHistoryReturnsFalse() {
        PricingCommandInvoker invoker = new PricingCommandInvoker();
        assertFalse(invoker.undo());
    }
}
