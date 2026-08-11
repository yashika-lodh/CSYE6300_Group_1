package com.csye6300.group1.pricingengine;

import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.channel.SalesChannel;
import com.csye6300.group1.pricingengine.channel.SalesChannelFactory;
import com.csye6300.group1.pricingengine.command.PricingCommandInvoker;
import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.forecast.DemandForecaster;
import com.csye6300.group1.pricingengine.inventory.Inventory;
import com.csye6300.group1.pricingengine.inventory.InventoryPersistenceObserver;
import com.csye6300.group1.pricingengine.inventory.InventoryRepository;
import com.csye6300.group1.pricingengine.selector.PricingStrategySelector;
import com.csye6300.group1.pricingengine.workflow.PricingInputsProvider;
import com.csye6300.group1.pricingengine.workflow.RepricingTriggerObserver;
import com.csye6300.group1.pricingengine.workflow.RepricingWorkflow;
import com.csye6300.group1.pricingengine.workflow.ScheduledRepricingWorkflow;
import com.csye6300.group1.pricingengine.workflow.StandardRepricingWorkflow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Demo driver: builds the full Dynamic Pricing Engine graph (Singleton
 * Inventory, Factory-built Shopify channel, Strategy + Selector,
 * Command + Decorator, Template Method workflow, Observer wiring) and runs a
 * sample repricing cycle triggered by a stock change. Milestone 3 adds a
 * DB-backed InventoryRepository (replacing the CSV) and a scheduled
 * repricing sweep on top of the same workflow hierarchy.
 */
public class PricingEngineDemo {

    public static void demo() throws InterruptedException {
        // 1. Singleton inventory, seeded from + kept in sync with the DB
        // (Milestone 3: InventoryRepository replaces the inventory.csv load)
        InventoryRepository inventoryRepository = new InventoryRepository();
        Inventory inventory = Inventory.getInstance();
        inventory.loadFromRepository(inventoryRepository);
        inventory.addObserver(new InventoryPersistenceObserver(inventoryRepository));

        inventory.updateStock("SKU-1001", 50);

        // 2. Factory Method -> channel creation
        SalesChannelFactory channelFactory = new SalesChannelFactory();
        SalesChannel shopify = channelFactory.createChannel(SalesChannelFactory.ChannelType.SHOPIFY);

        ChannelManager channelManager = new ChannelManager();
        channelManager.registerChannel(shopify);

        // 3. Sample demand history (simulating a rising trend)
        Map<String, List<DemandDataPoint>> demandHistory = new HashMap<>();
        demandHistory.put("SKU-1001", buildSampleRisingDemand("SKU-1001"));

        // 4. Pricing inputs (cost/competitor/aging) - CSV-backed in the full implementation
        PricingInputsProvider inputsProvider = new PricingInputsProvider() {
            @Override public double getCost(String sku) { return 15.00; }
            @Override public double getCurrentPrice(String sku) { return 24.99; }
            @Override public double getCompetitorPrice(String sku) { return 22.50; }
            @Override public int getDaysInInventory(String sku) { return 10; }
        };

        // 5. Wire the Template Method workflow with Strategy/Selector + Command/Decorator
        RepricingWorkflow workflow = new StandardRepricingWorkflow(
                demandHistory,
                inputsProvider,
                new DemandForecaster(),
                new PricingStrategySelector(),
                channelManager,
                new PricingCommandInvoker());

        // 6. Observer wiring: stock changes automatically trigger a reprice
        inventory.addObserver(new RepricingTriggerObserver(workflow));

        // 7. Simulate a sale, which fires the observer -> workflow.reprice("SKU-1001")
        inventory.adjustStock("SKU-1001", -5);

        System.out.println("Final Shopify price for SKU-1001: " + shopify.getPrice("SKU-1001"));

        // 8. Milestone 3: ScheduledRepricingWorkflow (a second Template Method
        // subclass) reprices SKU-1001 on a timer, independent of any stock change.
        ScheduledRepricingWorkflow scheduledWorkflow = new ScheduledRepricingWorkflow(
                demandHistory,
                inputsProvider,
                new DemandForecaster(),
                new PricingStrategySelector(),
                channelManager,
                new PricingCommandInvoker());
        scheduledWorkflow.startSchedule(List.of("SKU-1001"), 0, 1, TimeUnit.HOURS);
        Thread.sleep(200); // let the demo's first scheduled sweep run before shutting it down
        scheduledWorkflow.stopSchedule();

        inventoryRepository.close();
    }

    private static List<DemandDataPoint> buildSampleRisingDemand(String sku) {
        List<DemandDataPoint> history = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(30);
        for (int day = 0; day < 30; day++) {
            // last 7 days sell noticeably more than the 30-day baseline -> RISING
            double units = day >= 23 ? 20 : 10;
            history.add(new DemandDataPoint(sku, start.plusDays(day), units));
        }
        return history;
    }
}
