package com.csye6300.group1.pricingengine.selector;

import com.csye6300.group1.pricingengine.forecast.Trend;
import com.csye6300.group1.pricingengine.pricing.CompetitorAwarePricingStrategy;
import com.csye6300.group1.pricingengine.pricing.CostPlusMarkupStrategy;
import com.csye6300.group1.pricingengine.pricing.InventoryAgingStrategy;
import com.csye6300.group1.pricingengine.pricing.PricingStrategy;

/**
 * Decides which PricingStrategy (Strategy pattern) to use based on the
 * demand Trend produced by DemandForecaster:
 *   RISING  -> CompetitorAwarePricingStrategy (aggressive positioning)
 *   STABLE  -> CostPlusMarkupStrategy (margin optimization)
 *   FALLING -> InventoryAgingStrategy (clear old stock)
 */
public class PricingStrategySelector {

    private final PricingStrategy risingStrategy;
    private final PricingStrategy stableStrategy;
    private final PricingStrategy fallingStrategy;

    public PricingStrategySelector() {
        this(new CompetitorAwarePricingStrategy(), new CostPlusMarkupStrategy(), new InventoryAgingStrategy());
    }

    public PricingStrategySelector(PricingStrategy risingStrategy,
                                    PricingStrategy stableStrategy,
                                    PricingStrategy fallingStrategy) {
        this.risingStrategy = risingStrategy;
        this.stableStrategy = stableStrategy;
        this.fallingStrategy = fallingStrategy;
    }

    public PricingStrategy selectStrategy(Trend trend) {
        switch (trend) {
            case RISING:
                return risingStrategy;
            case FALLING:
                return fallingStrategy;
            case STABLE:
            default:
                return stableStrategy;
        }
    }
}
