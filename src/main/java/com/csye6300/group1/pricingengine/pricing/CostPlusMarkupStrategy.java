package com.csye6300.group1.pricingengine.pricing;

/**
 * Margin-optimization strategy used when demand is STABLE.
 * Price = cost * (1 + markupPercentage).
 */
public class CostPlusMarkupStrategy implements PricingStrategy {

    private final double markupPercentage;

    public CostPlusMarkupStrategy(double markupPercentage) {
        this.markupPercentage = markupPercentage;
    }

    public CostPlusMarkupStrategy() {
        this(0.35); // default 35% markup
    }

    @Override
    public double calculatePrice(PricingContext context) {
        double price = context.getCost() * (1 + markupPercentage);
        return round(price);
    }

    @Override
    public String getName() {
        return "CostPlusMarkupStrategy";
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
