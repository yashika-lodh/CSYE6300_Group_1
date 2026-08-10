package com.csye6300.group1.pricingengine.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring-bound config for pricing inputs (cost, competitor price, aging,
 * brand-positioning guardrails), read from application.properties instead of
 * being hardcoded in PricingEngineApplication. Same "config lives in
 * application.properties" convention already used for db.url/db.driver.
 *
 * "default*" fields apply to any SKU with no entry in "skus"; a SKU present
 * in "skus" overrides only the fields it sets, falling back to the defaults
 * for anything it leaves null.
 */
@ConfigurationProperties(prefix = "pricing")
public class PricingInputsProperties {

    private double defaultCost = 15.00;
    private double defaultCurrentPrice = 24.99;
    private double defaultCompetitorPrice = 22.50;
    private int defaultDaysInInventory = 10;
    private double defaultMinPrice = 0.0;
    private double defaultMaxPrice = 0.0;
    private Map<String, SkuOverrides> skus = new HashMap<>();

    public double getDefaultCost() { return defaultCost; }
    public void setDefaultCost(double defaultCost) { this.defaultCost = defaultCost; }

    public double getDefaultCurrentPrice() { return defaultCurrentPrice; }
    public void setDefaultCurrentPrice(double defaultCurrentPrice) { this.defaultCurrentPrice = defaultCurrentPrice; }

    public double getDefaultCompetitorPrice() { return defaultCompetitorPrice; }
    public void setDefaultCompetitorPrice(double defaultCompetitorPrice) { this.defaultCompetitorPrice = defaultCompetitorPrice; }

    public int getDefaultDaysInInventory() { return defaultDaysInInventory; }
    public void setDefaultDaysInInventory(int defaultDaysInInventory) { this.defaultDaysInInventory = defaultDaysInInventory; }

    public double getDefaultMinPrice() { return defaultMinPrice; }
    public void setDefaultMinPrice(double defaultMinPrice) { this.defaultMinPrice = defaultMinPrice; }

    public double getDefaultMaxPrice() { return defaultMaxPrice; }
    public void setDefaultMaxPrice(double defaultMaxPrice) { this.defaultMaxPrice = defaultMaxPrice; }

    public Map<String, SkuOverrides> getSkus() { return skus; }
    public void setSkus(Map<String, SkuOverrides> skus) { this.skus = skus; }

    /** Per-SKU overrides; any field left null falls back to the matching default* value above. */
    public static class SkuOverrides {
        private Double cost;
        private Double currentPrice;
        private Double competitorPrice;
        private Integer daysInInventory;
        private Double minPrice;
        private Double maxPrice;

        public Double getCost() { return cost; }
        public void setCost(Double cost) { this.cost = cost; }

        public Double getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }

        public Double getCompetitorPrice() { return competitorPrice; }
        public void setCompetitorPrice(Double competitorPrice) { this.competitorPrice = competitorPrice; }

        public Integer getDaysInInventory() { return daysInInventory; }
        public void setDaysInInventory(Integer daysInInventory) { this.daysInInventory = daysInInventory; }

        public Double getMinPrice() { return minPrice; }
        public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

        public Double getMaxPrice() { return maxPrice; }
        public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    }
}
