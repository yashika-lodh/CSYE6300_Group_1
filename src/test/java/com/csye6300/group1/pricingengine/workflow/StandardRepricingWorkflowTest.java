package com.csye6300.group1.pricingengine.workflow;

import com.csye6300.group1.pricingengine.audit.AuditLoggingCommandDecorator;
import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.channel.SalesChannel;
import com.csye6300.group1.pricingengine.command.PricingCommandInvoker;
import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.forecast.DemandForecaster;
import com.csye6300.group1.pricingengine.inventory.Inventory;
import com.csye6300.group1.pricingengine.selector.PricingStrategySelector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test for a gap where StandardRepricingWorkflow.buildPricingContext
 * built PricingContext with the no-guardrails constructor, so a
 * PricingInputsProvider's min/max price (e.g. from ConfigurablePricingInputsProvider,
 * backed by application.properties) was silently never enforced even though
 * every PricingStrategy already routes through PricingContext.applyGuardrails().
 */
class StandardRepricingWorkflowTest {

    private StandardRepricingWorkflow workflow;

    @AfterEach
    void tearDown() {
        Inventory.getInstance().resetForTesting();
    }

    @Test
    void repriceClampsToConfiguredMaxPrice() {
        String sku = "SKU-GUARDRAIL-MAX";
        Inventory.getInstance().updateStock(sku, 10);

        // STABLE trend (empty demand history) -> CostPlusMarkupStrategy:
        // cost 18.00 * 1.35 markup = 24.30, which the guardrail below must
        // clamp down to 20.00.
        RecordingFakeChannel channel = new RecordingFakeChannel();
        ChannelManager channelManager = new ChannelManager();
        channelManager.registerChannel(channel);

        workflow = new StandardRepricingWorkflow(
                Map.of(sku, List.<DemandDataPoint>of()),
                new PricingInputsProvider() {
                    @Override public double getCost(String s) { return 18.0; }
                    @Override public double getCurrentPrice(String s) { return 27.99; }
                    @Override public double getCompetitorPrice(String s) { return 26.50; }
                    @Override public int getDaysInInventory(String s) { return 5; }
                    @Override public double getMinPrice(String s) { return 0.0; }
                    @Override public double getMaxPrice(String s) { return 20.0; }
                },
                new DemandForecaster(),
                new PricingStrategySelector(),
                channelManager,
                new PricingCommandInvoker());

        workflow.reprice(sku);

        assertEquals(20.0, channel.getPrice(sku), 0.001,
                "Configured max-price guardrail should clamp the strategy's raw output");
    }

    @Test
    void repriceClampsToConfiguredMinPrice() {
        String sku = "SKU-GUARDRAIL-MIN";
        Inventory.getInstance().updateStock(sku, 10);

        // STABLE trend -> CostPlusMarkupStrategy: cost 8.00 * 1.35 = 10.80,
        // which the guardrail below must clamp up to 13.00.
        RecordingFakeChannel channel = new RecordingFakeChannel();
        ChannelManager channelManager = new ChannelManager();
        channelManager.registerChannel(channel);

        workflow = new StandardRepricingWorkflow(
                Map.of(sku, List.<DemandDataPoint>of()),
                new PricingInputsProvider() {
                    @Override public double getCost(String s) { return 8.0; }
                    @Override public double getCurrentPrice(String s) { return 14.99; }
                    @Override public double getCompetitorPrice(String s) { return 13.50; }
                    @Override public int getDaysInInventory(String s) { return 5; }
                    @Override public double getMinPrice(String s) { return 13.0; }
                    @Override public double getMaxPrice(String s) { return 0.0; }
                },
                new DemandForecaster(),
                new PricingStrategySelector(),
                channelManager,
                new PricingCommandInvoker());

        workflow.reprice(sku);

        assertEquals(13.0, channel.getPrice(sku), 0.001,
                "Configured min-price guardrail should clamp the strategy's raw output");
    }

    @Test
    void repriceLogsOneAuditEntryPerChannel() {
        String sku = "SKU-AUDIT-PER-CHANNEL";
        Inventory.getInstance().updateStock(sku, 10);

        ChannelManager channelManager = new ChannelManager();
        channelManager.registerChannel(new RecordingFakeChannel("Shopify"));
        channelManager.registerChannel(new RecordingFakeChannel("OwnWebsite"));
        channelManager.registerChannel(new RecordingFakeChannel("SocialCommerce"));

        workflow = new StandardRepricingWorkflow(
                Map.of(sku, List.<DemandDataPoint>of()),
                new PricingInputsProvider() {
                    @Override public double getCost(String s) { return 10.0; }
                    @Override public double getCurrentPrice(String s) { return 15.0; }
                    @Override public double getCompetitorPrice(String s) { return 14.0; }
                    @Override public int getDaysInInventory(String s) { return 5; }
                },
                new DemandForecaster(),
                new PricingStrategySelector(),
                channelManager,
                new PricingCommandInvoker());

        workflow.reprice(sku);

        long entriesForSku = AuditLoggingCommandDecorator.getGlobalAuditTrail().stream()
                .filter(entry -> entry.contains("sku=" + sku + " "))
                .count();

        assertEquals(3, entriesForSku,
                "Every channel's price change should get its own compliance-log entry, not one entry per reprice");
    }

    private static class RecordingFakeChannel implements SalesChannel {
        private final String name;
        private final Map<String, Double> prices = new ConcurrentHashMap<>();

        RecordingFakeChannel() {
            this("fake");
        }

        RecordingFakeChannel(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Inventory getInventory() {
            return Inventory.getInstance();
        }

        @Override
        public void publishPrice(String sku) {
            // no-op
        }

        @Override
        public void setPrice(String sku, double price) {
            prices.put(sku, price);
        }

        @Override
        public double getPrice(String sku) {
            return prices.getOrDefault(sku, 0.0);
        }
    }
}
