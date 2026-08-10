package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.channel.SalesChannelFactory;
import com.csye6300.group1.pricingengine.command.PricingCommandInvoker;
import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.forecast.DemandForecaster;
import com.csye6300.group1.pricingengine.forecast.DemandSignalRepository;
import com.csye6300.group1.pricingengine.inventory.Inventory;
import com.csye6300.group1.pricingengine.inventory.InventoryPersistenceObserver;
import com.csye6300.group1.pricingengine.inventory.InventoryRepository;
import com.csye6300.group1.pricingengine.selector.PricingStrategySelector;
import com.csye6300.group1.pricingengine.workflow.PricingInputsProvider;
import com.csye6300.group1.pricingengine.workflow.RepricingTriggerObserver;
import com.csye6300.group1.pricingengine.workflow.RepricingWorkflow;
import com.csye6300.group1.pricingengine.workflow.ScheduledRepricingWorkflow;
import com.csye6300.group1.pricingengine.workflow.StandardRepricingWorkflow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Milestone 3 entry point: wraps the same engine wired by hand in Main
 * (Singleton Inventory backed by InventoryRepository, Factory-built
 * channels, Strategy/Selector, Command/Decorator, Template Method workflow,
 * Observer) behind a Spring Boot REST API instead of a one-shot console
 * demo. Main.java is left untouched as the plain-Java demo driver.
 */
@SpringBootApplication
@EnableConfigurationProperties(PricingInputsProperties.class)
public class PricingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricingEngineApplication.class, args);
    }

    @Bean(destroyMethod = "close")
    public InventoryRepository inventoryRepository() {
        return new InventoryRepository();
    }

    @Bean
    public Inventory inventory(InventoryRepository inventoryRepository) {
        // Milestone 3: seed the Singleton from the DB-backed repository (replacing
        // the Milestone 2 CSV load), then register the same persistence Observer
        // Main.java uses, so stock changes made through InventoryController are
        // written through to the DB exactly like changes made by the console demo.
        Inventory inventory = Inventory.getInstance();
        inventory.loadFromRepository(inventoryRepository);
        inventory.addObserver(new InventoryPersistenceObserver(inventoryRepository));

        if (inventory.getStock("SKU-1001") == 0) {
            inventory.updateStock("SKU-1001", 50);
        }
        return inventory;
    }

    @Bean
    public ChannelManager channelManager() {
        SalesChannelFactory channelFactory = new SalesChannelFactory();
        ChannelManager channelManager = new ChannelManager();
        channelManager.registerChannel(channelFactory.createChannel(SalesChannelFactory.ChannelType.SHOPIFY));
        channelManager.registerChannel(channelFactory.createChannel(SalesChannelFactory.ChannelType.OWN_WEBSITE));
        channelManager.registerChannel(channelFactory.createChannel(SalesChannelFactory.ChannelType.SOCIAL_COMMERCE));
        return channelManager;
    }

    @Bean
    public Map<String, List<DemandDataPoint>> demandHistoryBySku() {
        // ConcurrentHashMap + CopyOnWriteArrayList (not the plain HashMap/ArrayList
        // DemandSignalRepository hands back) because this bean is no longer read-only
        // after startup: PricingInputsController and DemandController can add brand
        // new SKUs or append demand points to existing ones while reprice requests are
        // concurrently reading from the same map.
        Map<String, List<DemandDataPoint>> demandHistory = new ConcurrentHashMap<>();
        new DemandSignalRepository().loadDemandHistoryBySku()
                .forEach((sku, points) -> demandHistory.put(sku, new CopyOnWriteArrayList<>(points)));

        if (demandHistory.isEmpty()) {
            // CSV missing or empty (e.g. running from an unusual working directory) --
            // fall back to the Milestone 2 sample data so the app still starts and demos cleanly.
            demandHistory.put("SKU-1001", buildSampleRisingDemand("SKU-1001"));
        }
        return demandHistory;
    }

    // Reads cost/competitor-price/aging/guardrail values from application.properties
    // (pricing.default-* and pricing.skus.<SKU>.*) via PricingInputsProperties, instead
    // of hardcoding them here -- so tuning a guardrail no longer requires a rebuild.
    @Bean
    public PricingInputsProvider pricingInputsProvider(PricingInputsProperties properties) {
        return new ConfigurablePricingInputsProvider(properties);
    }

    @Bean
    public PricingCommandInvoker pricingCommandInvoker() {
        return new PricingCommandInvoker();
    }

    // @Primary: PricingController depends on RepricingWorkflow by type, and
    // scheduledRepricingWorkflow() below is also assignable to RepricingWorkflow
    // (ScheduledRepricingWorkflow extends StandardRepricingWorkflow). Marking the
    // reactive/on-demand workflow primary keeps that injection unambiguous.
    @Primary
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

    /**
     * Milestone 3: a second Template Method subclass that reprices every known
     * SKU on a fixed schedule, independent of any stock-change Observer event --
     * the same behavior Main.java demonstrates by hand, now running continuously
     * inside the REST app instead of only in the one-shot console demo.
     */
    @Bean(destroyMethod = "stopSchedule")
    public ScheduledRepricingWorkflow scheduledRepricingWorkflow(Map<String, List<DemandDataPoint>> demandHistoryBySku,
                                                                  PricingInputsProvider pricingInputsProvider,
                                                                  ChannelManager channelManager,
                                                                  PricingCommandInvoker pricingCommandInvoker) {
        ScheduledRepricingWorkflow workflow = new ScheduledRepricingWorkflow(
                demandHistoryBySku,
                pricingInputsProvider,
                new DemandForecaster(),
                new PricingStrategySelector(),
                channelManager,
                pricingCommandInvoker);

        workflow.startSchedule(demandHistoryBySku.keySet(), 0, 1, TimeUnit.HOURS);
        return workflow;
    }

    private static List<DemandDataPoint> buildSampleRisingDemand(String sku) {
        List<DemandDataPoint> history = new CopyOnWriteArrayList<>();
        LocalDate start = LocalDate.now().minusDays(30);
        for (int day = 0; day < 30; day++) {
            double units = day >= 23 ? 20 : 10;
            history.add(new DemandDataPoint(sku, start.plusDays(day), units));
        }
        return history;
    }
}