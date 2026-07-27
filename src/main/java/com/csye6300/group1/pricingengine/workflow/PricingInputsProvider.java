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
}
