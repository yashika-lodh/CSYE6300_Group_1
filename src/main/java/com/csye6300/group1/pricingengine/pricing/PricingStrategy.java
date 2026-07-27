package com.csye6300.group1.pricingengine.pricing;

/**
 * Strategy pattern: interchangeable pricing algorithms. PricingStrategySelector
 * picks the concrete implementation at runtime based on the current demand trend.
 */
public interface PricingStrategy {
    double calculatePrice(PricingContext context);

    /** Human-readable name used in audit logs and reports. */
    String getName();
}
