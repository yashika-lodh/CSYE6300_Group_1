package com.csye6300.group1.pricingengine.workflow;

/**
 * Read-only result of {@link RepricingWorkflow#previewForecast(String)}: which
 * demand Trend was detected and which PricingStrategy it maps to, without
 * actually executing a price change.
 */
public record ForecastInsight(String trend, String strategyName) {
}
