package com.csye6300.group1.pricingengine.pricing;

/**
 * Aggressive-positioning strategy used when demand is RISING.
 * Undercuts the competitor price slightly while never pricing below cost.
 */
public class CompetitorAwarePricingStrategy implements PricingStrategy {

    private final double undercutPercentage;

    public CompetitorAwarePricingStrategy(double undercutPercentage) {
        this.undercutPercentage = undercutPercentage;
    }

    public CompetitorAwarePricingStrategy() {
        this(0.05); // undercut competitor by 5% by default
    }

    @Override
    public double calculatePrice(PricingContext context) {
        double target = context.getCompetitorPrice() * (1 - undercutPercentage);
        double floor = context.getCost() * 1.10; // never price below 10% margin
        double price = Math.max(target, floor);
        return round(context.applyGuardrails(price));
    }

    @Override
    public String getName() {
        return "CompetitorAwarePricingStrategy";
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
