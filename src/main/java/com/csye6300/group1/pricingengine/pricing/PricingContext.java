package com.csye6300.group1.pricingengine.pricing;

/**
 * Carries every input a PricingStrategy might need. Keeping this as a single
 * context object means new strategies can be added without changing the
 * PricingStrategy interface signature.
 */
public final class PricingContext {

    private final String sku;
    private final double cost;
    private final double currentPrice;
    private final double competitorPrice;
    private final int daysInInventory;
    private final int unitsInStock;
    private final double minPrice;
    private final double maxPrice;

    /**
     * Full constructor including brand-positioning guardrails. Pass
     * {@code minPrice <= 0} to leave the floor unset, and
     * {@code maxPrice <= 0} to leave the ceiling unset.
     */
    public PricingContext(String sku, double cost, double currentPrice,
                           double competitorPrice, int daysInInventory, int unitsInStock,
                           double minPrice, double maxPrice) {
        this.sku = sku;
        this.cost = cost;
        this.currentPrice = currentPrice;
        this.competitorPrice = competitorPrice;
        this.daysInInventory = daysInInventory;
        this.unitsInStock = unitsInStock;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    /**
     * Milestone 2 constructor, preserved for existing call sites and tests.
     * No brand-positioning guardrails are applied.
     */
    public PricingContext(String sku, double cost, double currentPrice,
                           double competitorPrice, int daysInInventory, int unitsInStock) {
        this(sku, cost, currentPrice, competitorPrice, daysInInventory, unitsInStock, 0.0, 0.0);
    }

    public String getSku() { return sku; }
    public double getCost() { return cost; }
    public double getCurrentPrice() { return currentPrice; }
    public double getCompetitorPrice() { return competitorPrice; }
    public int getDaysInInventory() { return daysInInventory; }
    public int getUnitsInStock() { return unitsInStock; }
    public double getMinPrice() { return minPrice; }
    public double getMaxPrice() { return maxPrice; }

    public boolean hasMinPrice() { return minPrice > 0; }
    public boolean hasMaxPrice() { return maxPrice > 0; }

    /**
     * Clamps a candidate price into the configured brand-positioning bounds.
     * Every PricingStrategy should route its final price through this before
     * rounding, so guardrails apply uniformly regardless of which strategy
     * is active.
     */
    public double applyGuardrails(double price) {
        double result = price;
        if (hasMinPrice()) {
            result = Math.max(result, minPrice);
        }
        if (hasMaxPrice()) {
            result = Math.min(result, maxPrice);
        }
        return result;
    }
}