package com.csye6300.group1.pricingengine.pricing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingStrategyTest {

    @Test
    void costPlusMarkupAppliesConfiguredMarkup() {
        CostPlusMarkupStrategy strategy = new CostPlusMarkupStrategy(0.5); // 50%
        PricingContext context = new PricingContext("SKU-1", 10.0, 20.0, 18.0, 5, 100);
        assertEquals(15.0, strategy.calculatePrice(context));
    }

    @Test
    void competitorAwareUndercutsCompetitorPrice() {
        CompetitorAwarePricingStrategy strategy = new CompetitorAwarePricingStrategy(0.10); // undercut 10%
        PricingContext context = new PricingContext("SKU-1", 10.0, 20.0, 30.0, 5, 100);
        assertEquals(27.0, strategy.calculatePrice(context));
    }

    @Test
    void competitorAwareNeverPricesBelowMarginFloor() {
        CompetitorAwarePricingStrategy strategy = new CompetitorAwarePricingStrategy(0.90); // huge undercut
        PricingContext context = new PricingContext("SKU-1", 10.0, 20.0, 12.0, 5, 100);
        // 12 * 0.10 = 1.20 target, but floor is 10 * 1.10 = 11.00
        assertEquals(11.0, strategy.calculatePrice(context));
    }

    @Test
    void inventoryAgingDiscountsMoreTheLongerStockSits() {
        InventoryAgingStrategy strategy = new InventoryAgingStrategy();
        PricingContext freshStock = new PricingContext("SKU-1", 10.0, 100.0, 90.0, 0, 50);
        PricingContext agedStock = new PricingContext("SKU-1", 10.0, 100.0, 90.0, 30, 50);

        double freshPrice = strategy.calculatePrice(freshStock);
        double agedPrice = strategy.calculatePrice(agedStock);

        assertEquals(100.0, freshPrice);
        assertTrue(agedPrice < freshPrice, "Aged stock should be discounted more than fresh stock");
    }

    @Test
    void inventoryAgingNeverDropsBelowMarginFloor() {
        InventoryAgingStrategy strategy = new InventoryAgingStrategy();
        PricingContext context = new PricingContext("SKU-1", 10.0, 12.0, 8.0, 90, 5);
        assertTrue(strategy.calculatePrice(context) >= 10.5); // cost * 1.05
    }

    @Test
    void guardrailCeilingCapsCostPlusMarkupPrice() {
        CostPlusMarkupStrategy strategy = new CostPlusMarkupStrategy(0.5); // would be 15.0 uncapped
        PricingContext context = new PricingContext("SKU-1", 10.0, 20.0, 18.0, 5, 100, 0.0, 13.0);
        assertEquals(13.0, strategy.calculatePrice(context));
    }

    @Test
    void guardrailFloorRaisesCompetitorAwarePriceAboveMarginFloor() {
        CompetitorAwarePricingStrategy strategy = new CompetitorAwarePricingStrategy(0.10);
        // Undercut would land at 27.0, well above the brand-positioning floor of 29.0.
        PricingContext context = new PricingContext("SKU-1", 10.0, 20.0, 30.0, 5, 100, 29.0, 0.0);
        assertEquals(29.0, strategy.calculatePrice(context));
    }

    @Test
    void guardrailsDoNotAffectStrategiesWhenUnset() {
        CostPlusMarkupStrategy strategy = new CostPlusMarkupStrategy(0.5);
        PricingContext withoutGuardrails = new PricingContext("SKU-1", 10.0, 20.0, 18.0, 5, 100);
        PricingContext withDisabledGuardrails = new PricingContext("SKU-1", 10.0, 20.0, 18.0, 5, 100, 0.0, 0.0);
        assertEquals(strategy.calculatePrice(withoutGuardrails), strategy.calculatePrice(withDisabledGuardrails));
    }
}
