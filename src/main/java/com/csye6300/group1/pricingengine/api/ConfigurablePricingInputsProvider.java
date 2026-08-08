package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.workflow.PricingInputsProvider;

import java.util.function.Function;

/**
 * PricingInputsProvider backed by PricingInputsProperties, so cost,
 * competitor price, aging, and brand-positioning guardrails can be tuned via
 * application.properties (or a per-SKU override) instead of being hardcoded
 * in PricingEngineApplication and requiring a rebuild to change.
 */
public class ConfigurablePricingInputsProvider implements PricingInputsProvider {

    private final PricingInputsProperties properties;

    public ConfigurablePricingInputsProvider(PricingInputsProperties properties) {
        this.properties = properties;
    }

    @Override
    public double getCost(String sku) {
        return resolve(sku, PricingInputsProperties.SkuOverrides::getCost, properties.getDefaultCost());
    }

    @Override
    public double getCurrentPrice(String sku) {
        return resolve(sku, PricingInputsProperties.SkuOverrides::getCurrentPrice, properties.getDefaultCurrentPrice());
    }

    @Override
    public double getCompetitorPrice(String sku) {
        return resolve(sku, PricingInputsProperties.SkuOverrides::getCompetitorPrice, properties.getDefaultCompetitorPrice());
    }

    @Override
    public int getDaysInInventory(String sku) {
        return resolve(sku, PricingInputsProperties.SkuOverrides::getDaysInInventory, properties.getDefaultDaysInInventory());
    }

    @Override
    public double getMinPrice(String sku) {
        return resolve(sku, PricingInputsProperties.SkuOverrides::getMinPrice, properties.getDefaultMinPrice());
    }

    @Override
    public double getMaxPrice(String sku) {
        return resolve(sku, PricingInputsProperties.SkuOverrides::getMaxPrice, properties.getDefaultMaxPrice());
    }

    private <T> T resolve(String sku, Function<PricingInputsProperties.SkuOverrides, T> field, T defaultValue) {
        PricingInputsProperties.SkuOverrides overrides = properties.getSkus().get(sku);
        if (overrides == null) {
            return defaultValue;
        }
        T value = field.apply(overrides);
        return value != null ? value : defaultValue;
    }
}
