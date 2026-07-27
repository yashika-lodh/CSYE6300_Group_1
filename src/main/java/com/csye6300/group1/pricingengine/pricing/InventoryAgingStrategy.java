package com.csye6300.group1.pricingengine.pricing;

/**
 * Clear-old-stock strategy used when demand is FALLING.
 * Applies an increasing discount the longer a SKU has sat in inventory.
 */
public class InventoryAgingStrategy implements PricingStrategy {

    private static final int DAYS_THRESHOLD = 30;
    private static final double MAX_DISCOUNT = 0.40;

    @Override
    public double calculatePrice(PricingContext context) {
        double agingFactor = Math.min(1.0, context.getDaysInInventory() / (double) DAYS_THRESHOLD);
        double discount = agingFactor * MAX_DISCOUNT;
        double price = context.getCurrentPrice() * (1 - discount);
        double floor = context.getCost() * 1.05; // never sell below 5% margin
        return round(Math.max(price, floor));
    }

    @Override
    public String getName() {
        return "InventoryAgingStrategy";
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
