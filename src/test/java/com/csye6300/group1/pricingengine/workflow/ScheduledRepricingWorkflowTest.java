package com.csye6300.group1.pricingengine.workflow;

import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.channel.SalesChannel;
import com.csye6300.group1.pricingengine.command.PricingCommandInvoker;
import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.forecast.DemandForecaster;
import com.csye6300.group1.pricingengine.inventory.Inventory;
import com.csye6300.group1.pricingengine.selector.PricingStrategySelector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Milestone 3 ScheduledRepricingWorkflow reprices a SKU
 * repeatedly on its own timer, with no Inventory stock change (and
 * therefore no Observer notification) involved at all.
 */
class ScheduledRepricingWorkflowTest {

    private ScheduledRepricingWorkflow workflow;

    @AfterEach
    void tearDown() {
        if (workflow != null) {
            workflow.stopSchedule();
        }
        Inventory.getInstance().resetForTesting();
    }

    @Test
    void scheduleRepricesSkuRepeatedlyWithoutAnyStockChangeEvent() throws InterruptedException {
        String sku = "SKU-SCHED";
        Inventory.getInstance().updateStock(sku, 20);

        CountDownLatch repriced = new CountDownLatch(3);
        SalesChannel channel = new CountingFakeChannel(repriced);

        ChannelManager channelManager = new ChannelManager();
        channelManager.registerChannel(channel);

        Map<String, List<DemandDataPoint>> demandHistory = Map.of(sku, buildRisingDemand(sku));

        workflow = new ScheduledRepricingWorkflow(
                demandHistory,
                new PricingInputsProvider() {
                    @Override public double getCost(String s) { return 10.0; }
                    @Override public double getCurrentPrice(String s) { return 20.0; }
                    @Override public double getCompetitorPrice(String s) { return 18.0; }
                    @Override public int getDaysInInventory(String s) { return 5; }
                },
                new DemandForecaster(),
                new PricingStrategySelector(),
                channelManager,
                new PricingCommandInvoker());

        workflow.startSchedule(List.of(sku), 0, 20, TimeUnit.MILLISECONDS);

        assertTrue(repriced.await(2, TimeUnit.SECONDS),
                "Scheduled workflow should reprice the SKU repeatedly on its own timer");
    }

    @Test
    void startScheduleTwiceThrows() {
        workflow = new ScheduledRepricingWorkflow(
                Map.of(),
                new PricingInputsProvider() {
                    @Override public double getCost(String s) { return 10.0; }
                    @Override public double getCurrentPrice(String s) { return 20.0; }
                    @Override public double getCompetitorPrice(String s) { return 18.0; }
                    @Override public int getDaysInInventory(String s) { return 5; }
                },
                new DemandForecaster(),
                new PricingStrategySelector(),
                new ChannelManager(),
                new PricingCommandInvoker());

        workflow.startSchedule(List.of("SKU-1"), 0, 1, TimeUnit.HOURS);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> workflow.startSchedule(List.of("SKU-1"), 0, 1, TimeUnit.HOURS));
    }

    private static List<DemandDataPoint> buildRisingDemand(String sku) {
        List<DemandDataPoint> history = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(30);
        for (int day = 0; day < 30; day++) {
            double units = day >= 23 ? 20 : 10;
            history.add(new DemandDataPoint(sku, start.plusDays(day), units));
        }
        return history;
    }

    private static class CountingFakeChannel implements SalesChannel {
        private final Map<String, Double> prices = new ConcurrentHashMap<>();
        private final CountDownLatch latch;

        CountingFakeChannel(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public String getName() {
            return "fake";
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
            latch.countDown();
        }

        @Override
        public double getPrice(String sku) {
            return prices.getOrDefault(sku, 0.0);
        }
    }
}
