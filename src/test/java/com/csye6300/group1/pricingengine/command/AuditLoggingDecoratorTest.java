package com.csye6300.group1.pricingengine.command;

import com.csye6300.group1.pricingengine.audit.AuditLoggingCommandDecorator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLoggingDecoratorTest {

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
    void decoratorDelegatesExecuteToWrappedCommand() {
        InMemoryPriceReceiver receiver = new InMemoryPriceReceiver();
        receiver.setPrice("SKU-1", 10.0);
        AuditLoggingCommandDecorator decorated =
                new AuditLoggingCommandDecorator(new UpdatePriceCommand(receiver, "SKU-1", 20.0));

        decorated.execute();

        assertEquals(20.0, receiver.getPrice("SKU-1"));
    }

    @Test
    void decoratorRecordsAnAuditEntryPerAction() {
        InMemoryPriceReceiver receiver = new InMemoryPriceReceiver();
        receiver.setPrice("SKU-1", 10.0);
        AuditLoggingCommandDecorator decorated =
                new AuditLoggingCommandDecorator(new UpdatePriceCommand(receiver, "SKU-1", 20.0));

        decorated.execute();
        decorated.undo();

        assertEquals(2, decorated.generateAuditReport().size());
        assertTrue(decorated.generateAuditReport().get(0).contains("EXECUTE"));
        assertTrue(decorated.generateAuditReport().get(1).contains("UNDO"));
    }
}
