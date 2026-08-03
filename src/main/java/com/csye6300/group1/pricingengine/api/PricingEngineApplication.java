package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.channel.SalesChannelFactory;
import com.csye6300.group1.pricingengine.command.PricingCommandInvoker;
import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.forecast.DemandForecaster;
import com.csye6300.group1.pricingengine.inventory.Inventory;
import com.csye6300.group1.pricingengine.selector.PricingStrategySelector;
import com.csye6300.group1.pricingengine.workflow.PricingInputsProvider;
import com.csye6300.group1.pricingengine.workflow.RepricingTriggerObserver;
import com.csye6300.group1.pricingengine.workflow.RepricingWorkflow;
import com.csye6300.group1.pricingengine.workflow.StandardRepricingWorkflow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milestone 3 entry point: wraps the same engine wired by hand in Main
 * (Singleton Inventory, Factory-built channels, Strategy/Selector,
 * Command/Decorator, Template Method workflow, Observer) behind a Spring
 * Boot REST API instead of a one-shot console demo. Main.java is left
 * untouched as the plain-Java demo driver.
 */
@SpringBootApplication
public class PricingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricingEngineApplication.class, args);
    }

    @Bean
    public Inventory inventory() {
        Inventory inventory = Inventory.getInstance();
        if (inventory.getStock("SKU-1001") == 0) {
            inventory.updateStock("SKU-1001", 50);
        }
        return inventory;
    }

    @Bean
    public ChannelManager channelManager() {
        ChannelManager channelManager = new ChannelManager();
        channelManager.registerChannel(
                new SalesChannelFactory().createChannel(SalesChannelFactory.ChannelType.SHOPIFY));
        return channelManager;
    }

    @Bean
    public Map<String, List<DemandDataPoint>> demandHistoryBySku() {
        Map<String, List<DemandDataPoint>> demandHistory = new HashMap<>();
        demandHistory.put("SKU-1001", buildSampleRisingDemand("SKU-1001"));
        return demandHistory;
    }

    @Bean
    public PricingInputsProvider pricingInputsProvider() {
        return new PricingInputsProvider() {
            @Override public double getCost(String sku) { return 15.00; }
            @Override public double getCurrentPrice(String sku) { return 24.99; }
            @Override public double getCompetitorPrice(String sku) { return 22.50; }
            @Override public int getDaysInInventory(String sku) { return 10; }
        };
    }

    @Bean
    public PricingCommandInvoker pricingCommandInvoker() {
        return new PricingCommandInvoker();
    }

    @Bean
    public RepricingWorkflow repricingWorkflow(Inventory inventory,
                                                Map<String, List<DemandDataPoint>> demandHistoryBySku,
                                                PricingInputsProvider pricingInputsProvider,
                                                ChannelManager channelManager,
                                                PricingCommandInvoker pricingCommandInvoker) {
        RepricingWorkflow workflow = new StandardRepricingWorkflow(
                demandHistoryBySku,
                pricingInputsProvider,
                new DemandForecaster(),
                new PricingStrategySelector(),
                channelManager,
                pricingCommandInvoker);

        // Same Observer wiring as Main: a stock change (e.g. via InventoryController)
        // automatically triggers a reprice, in addition to the manual /reprice endpoint.
        inventory.addObserver(new RepricingTriggerObserver(workflow));
        return workflow;
    }

    private static List<DemandDataPoint> buildSampleRisingDemand(String sku) {
        List<DemandDataPoint> history = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(30);
        for (int day = 0; day < 30; day++) {
            double units = day >= 23 ? 20 : 10;
            history.add(new DemandDataPoint(sku, start.plusDays(day), units));
        }
        return history;
    }
}
