package com.csye6300.group1.pricingengine.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies ConfigurablePricingInputsProvider's fallback rule: a SKU with no
 * entry in "skus" gets the "default*" values, and a SKU with an entry only
 * overrides the fields it actually sets.
 */
class ConfigurablePricingInputsProviderTest {

    @Test
    void unknownSkuFallsBackToDefaults() {
        PricingInputsProperties properties = new PricingInputsProperties();
        properties.setDefaultCost(15.00);
        properties.setDefaultCurrentPrice(24.99);
        properties.setDefaultCompetitorPrice(22.50);
        properties.setDefaultDaysInInventory(10);
        properties.setDefaultMinPrice(0.0);
        properties.setDefaultMaxPrice(0.0);

        ConfigurablePricingInputsProvider provider = new ConfigurablePricingInputsProvider(properties);

        assertEquals(15.00, provider.getCost("SKU-UNKNOWN"));
        assertEquals(24.99, provider.getCurrentPrice("SKU-UNKNOWN"));
        assertEquals(22.50, provider.getCompetitorPrice("SKU-UNKNOWN"));
        assertEquals(10, provider.getDaysInInventory("SKU-UNKNOWN"));
        assertEquals(0.0, provider.getMinPrice("SKU-UNKNOWN"));
        assertEquals(0.0, provider.getMaxPrice("SKU-UNKNOWN"));
    }

    @Test
    void skuOverrideOnlyReplacesFieldsItSets() {
        PricingInputsProperties properties = new PricingInputsProperties();
        properties.setDefaultCost(15.00);
        properties.setDefaultCurrentPrice(24.99);
        properties.setDefaultCompetitorPrice(22.50);
        properties.setDefaultDaysInInventory(10);

        PricingInputsProperties.SkuOverrides overrides = new PricingInputsProperties.SkuOverrides();
        overrides.setMinPrice(21.50);
        overrides.setMaxPrice(25.00);
        properties.setSkus(Map.of("SKU-1001", overrides));

        ConfigurablePricingInputsProvider provider = new ConfigurablePricingInputsProvider(properties);

        // Guardrails come from the override...
        assertEquals(21.50, provider.getMinPrice("SKU-1001"));
        assertEquals(25.00, provider.getMaxPrice("SKU-1001"));
        // ...but cost/current/competitor/aging fall back to the defaults since
        // the override left them unset.
        assertEquals(15.00, provider.getCost("SKU-1001"));
        assertEquals(24.99, provider.getCurrentPrice("SKU-1001"));
        assertEquals(22.50, provider.getCompetitorPrice("SKU-1001"));
        assertEquals(10, provider.getDaysInInventory("SKU-1001"));
    }

    @Test
    void skuOverrideCanReplaceEveryField() {
        PricingInputsProperties properties = new PricingInputsProperties();

        PricingInputsProperties.SkuOverrides overrides = new PricingInputsProperties.SkuOverrides();
        overrides.setCost(5.00);
        overrides.setCurrentPrice(9.99);
        overrides.setCompetitorPrice(8.50);
        overrides.setDaysInInventory(2);
        properties.setSkus(Map.of("SKU-2002", overrides));

        ConfigurablePricingInputsProvider provider = new ConfigurablePricingInputsProvider(properties);

        assertEquals(5.00, provider.getCost("SKU-2002"));
        assertEquals(9.99, provider.getCurrentPrice("SKU-2002"));
        assertEquals(8.50, provider.getCompetitorPrice("SKU-2002"));
        assertEquals(2, provider.getDaysInInventory("SKU-2002"));
    }
}
