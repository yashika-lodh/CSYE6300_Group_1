package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.inventory.Inventory;
import com.csye6300.group1.pricingengine.workflow.RepricingWorkflow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class DemoDataSeeder {

    private static final long STEP_DELAY_MS = 300;
    private static final int STEPS_PER_SKU = 3;

    private final Inventory inventory;
    private final PricingInputsProperties pricingInputsProperties;
    private final Map<String, List<DemandDataPoint>> demandHistoryBySku;
    private final RepricingWorkflow repricingWorkflow;

    public DemoDataSeeder(Inventory inventory,
                           PricingInputsProperties pricingInputsProperties,
                           Map<String, List<DemandDataPoint>> demandHistoryBySku,
                           RepricingWorkflow repricingWorkflow) {
        this.inventory = inventory;
        this.pricingInputsProperties = pricingInputsProperties;
        this.demandHistoryBySku = demandHistoryBySku;
        this.repricingWorkflow = repricingWorkflow;
    }

    public List<String> seed() {
        List<String> skus = new ArrayList<>();

        // STABLE -> CostPlusMarkupStrategy, no guardrail.
        skus.add(seedOne("PCH-1042", "Fast-Charge USB-C Cable", 10.0, 20.0, 22.0, 5, 0.0, 0.0, null));
        // RISING (fed demand) -> CompetitorAwarePricingStrategy.
        skus.add(seedOne("SNK-2210", "Trail Runner Sneakers", 18.0, 35.0, 42.0, 5, 0.0, 0.0, new double[] {32, 34, 33, 36, 38}));
        // FALLING (fed demand) -> InventoryAgingStrategy.
        skus.add(seedOne("JKT-3305", "Packable Rain Jacket", 25.0, 45.0, 40.0, 45, 0.0, 0.0, new double[] {2, 3, 2, 1, 2}));
        // STABLE, but cost-plus would land above this max -> pinned at the ceiling.
        skus.add(seedOne("WCH-4090", "Minimalist Analog Watch", 30.0, 50.0, 60.0, 5, 0.0, 40.0, null));
        // STABLE, but cost-plus would land below this min -> pinned at the floor.
        skus.add(seedOne("SGL-5015", "Polarized Sunglasses", 6.0, 12.0, 14.0, 5, 10.0, 0.0, null));

        return skus;
    }

    private String seedOne(String sku, String name, double cost, double currentPrice, double competitorPrice,
                            int daysInInventory, double minPrice, double maxPrice, double[] demandUnits) {
        pricingInputsProperties.upsertSkuOverride(sku, cost, currentPrice, competitorPrice, daysInInventory, minPrice, maxPrice, name);
        demandHistoryBySku.computeIfAbsent(sku, DemoDataSeeder::flatBaselineDemand);
        if (demandUnits != null) {
            appendDemand(sku, demandUnits);
        }
        if (inventory.getStock(sku) == 0) {
            inventory.updateStock(sku, 25);
        }
        repriceWithDrift(sku, cost, competitorPrice);
        return sku;
    }

    private void appendDemand(String sku, double[] units) {
        List<DemandDataPoint> history = demandHistoryBySku.computeIfAbsent(sku, k -> new CopyOnWriteArrayList<>());
        LocalDate today = LocalDate.now();
        for (double units_ : units) {
            history.add(new DemandDataPoint(sku, today, units_));
        }
    }

    /**
     * Reprices a SKU a few times with a short real delay, drifting cost and
     * competitor price slightly upward between each -- enough to move a
     * cost-plus or competitor-aware price to a genuinely different point,
     * while a guardrail-clamped SKU correctly stays pinned at its bound
     * regardless (a real, honest demonstration of the clamp holding).
     */
    private void repriceWithDrift(String sku, double startingCost, double startingCompetitorPrice) {
        double cost = startingCost;
        double competitorPrice = startingCompetitorPrice;
        for (int i = 0; i < STEPS_PER_SKU; i++) {
            repricingWorkflow.reprice(sku);
            try {
                Thread.sleep(STEP_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            cost *= 1.04;
            competitorPrice *= 1.06;
            pricingInputsProperties.upsertSkuOverride(sku, cost, null, competitorPrice, null, null, null);
        }
    }

    private static List<DemandDataPoint> flatBaselineDemand(String sku) {
        List<DemandDataPoint> history = new CopyOnWriteArrayList<>();
        LocalDate start = LocalDate.now().minusDays(5);
        for (int day = 0; day < 6; day++) {
            history.add(new DemandDataPoint(sku, start.plusDays(day), 10.0));
        }
        return history;
    }
}
