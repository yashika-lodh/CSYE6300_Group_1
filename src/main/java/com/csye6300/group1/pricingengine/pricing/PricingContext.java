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

    public PricingContext(String sku, double cost, double currentPrice,
                           double competitorPrice, int daysInInventory, int unitsInStock) {
        this.sku = sku;
        this.cost = cost;
        this.currentPrice = currentPrice;
        this.competitorPrice = competitorPrice;
        this.daysInInventory = daysInInventory;
        this.unitsInStock = unitsInStock;
    }

    public String getSku() { return sku; }
    public double getCost() { return cost; }
    public double getCurrentPrice() { return currentPrice; }
    public double getCompetitorPrice() { return competitorPrice; }
    public int getDaysInInventory() { return daysInInventory; }
    public int getUnitsInStock() { return unitsInStock; }
}
