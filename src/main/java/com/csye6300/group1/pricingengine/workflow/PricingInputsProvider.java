package com.csye6300.group1.pricingengine.workflow;

/**
 * Small seam so StandardRepricingWorkflow doesn't need to know where cost,
 * competitor price, and aging data come from (CSV today, a pricing DB or
 * competitor-scraping service in Milestone 3).
 */
public interface PricingInputsProvider {
    double getCost(String sku);
    double getCurrentPrice(String sku);
    double getCompetitorPrice(String sku);
    int getDaysInInventory(String sku);

    /**
     * Brand-positioning price floor for the SKU (Milestone 3). Defaults to
     * unset (0) so existing implementations don't need to change; override
     * to have PricingContext.applyGuardrails() actually clamp strategy output.
     */
    default double getMinPrice(String sku) {
        return 0.0;
    }

    /** Brand-positioning price ceiling for the SKU (Milestone 3). Defaults to unset (0). */
    default double getMaxPrice(String sku) {
        return 0.0;
    }
}
