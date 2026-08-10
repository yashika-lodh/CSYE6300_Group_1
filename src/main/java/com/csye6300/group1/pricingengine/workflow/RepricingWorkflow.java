package com.csye6300.group1.pricingengine.workflow;

import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.pricing.PricingContext;

import java.util.List;
import java.util.logging.Logger;

/**
 * Template Method pattern: defines the fixed skeleton every repricing run
 * follows -- fetch -> validate -> execute -> log -- while letting subclasses
 * (or, in Milestone 3, different workflow variants such as a
 * "ScheduledRepricingWorkflow") override individual steps.
 */
public abstract class RepricingWorkflow {

    protected final Logger logger = Logger.getLogger(getClass().getName());

    /** The template method. Marked final so the overall sequence can't be reordered by subclasses. */
    public final void reprice(String sku) {
        List<DemandDataPoint> demandHistory = fetchDemandHistory(sku);

        if (!validate(sku, demandHistory)) {
            logger.warning("Validation failed for " + sku + "; skipping reprice.");
            return;
        }

        PricingContext context = buildPricingContext(sku, demandHistory);
        double newPrice = execute(sku, context, demandHistory);

        log(sku, newPrice);
    }

    /**
     * Read-only preview of what a reprice would decide -- which Trend was
     * detected and which PricingStrategy it maps to -- without executing any
     * command or changing a price. Lets a dashboard show "why" a SKU would be
     * repriced a certain way before (or instead of) actually triggering it.
     */
    public final ForecastInsight previewForecast(String sku) {
        List<DemandDataPoint> demandHistory = fetchDemandHistory(sku);
        return computeForecastInsight(demandHistory);
    }

    protected abstract List<DemandDataPoint> fetchDemandHistory(String sku);

    protected abstract ForecastInsight computeForecastInsight(List<DemandDataPoint> demandHistory);

    /**
     * Hook with a sensible default: only a missing (null) history list is invalid.
     * An empty list is valid too -- DemandForecaster.forecastTrend() already
     * treats "no history yet" as STABLE, so a brand-new SKU with no demand
     * signal still gets priced (from PricingInputsProvider's defaults) on its
     * first reprice, instead of every channel silently staying unset. Subclasses
     * may tighten this further.
     */
    protected boolean validate(String sku, List<DemandDataPoint> demandHistory) {
        return demandHistory != null;
    }

    protected abstract PricingContext buildPricingContext(String sku, List<DemandDataPoint> demandHistory);

    /** Runs strategy selection + price calculation + command execution; returns the new price. */
    protected abstract double execute(String sku, PricingContext context, List<DemandDataPoint> demandHistory);

    protected void log(String sku, double newPrice) {
        logger.info("Repriced " + sku + " -> " + newPrice);
    }
}
