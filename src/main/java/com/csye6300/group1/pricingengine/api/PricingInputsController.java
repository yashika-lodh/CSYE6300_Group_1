package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.workflow.PricingInputsProvider;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Onboards a SKU (or edits an existing one) with the pricing inputs every
 * PricingStrategy reads: cost, competitor price, current price, days in
 * inventory, and brand-positioning guardrails. Backed by the same
 * PricingInputsProperties that application.properties populates at startup,
 * so a value set here takes effect on the very next reprice with no restart.
 *
 * A brand new SKU (one with no demand history yet) would otherwise fail the
 * Template Method's validate() step and never get priced -- exactly what
 * happens to a SKU that only ever gets an inventory row. Configuring pricing
 * inputs here also seeds a flat baseline demand history for it, the same
 * shape recorded demand later builds on via DemandController.
 */
@RestController
@RequestMapping("/api/pricing-inputs")
public class PricingInputsController {

    private static final int BASELINE_DAYS = 6;
    private static final double BASELINE_UNITS = 10.0;

    private final PricingInputsProperties properties;
    private final PricingInputsProvider pricingInputsProvider;
    private final Map<String, List<DemandDataPoint>> demandHistoryBySku;

    public PricingInputsController(PricingInputsProperties properties,
                                    PricingInputsProvider pricingInputsProvider,
                                    Map<String, List<DemandDataPoint>> demandHistoryBySku) {
        this.properties = properties;
        this.pricingInputsProvider = pricingInputsProvider;
        this.demandHistoryBySku = demandHistoryBySku;
    }

    /** Creates or updates a SKU's pricing inputs; only the parameters supplied are changed. */
    @PutMapping("/{sku}")
    public Map<String, Object> upsert(@PathVariable String sku,
                                       @RequestParam(required = false) Double cost,
                                       @RequestParam(required = false) Double currentPrice,
                                       @RequestParam(required = false) Double competitorPrice,
                                       @RequestParam(required = false) Integer daysInInventory,
                                       @RequestParam(required = false) Double minPrice,
                                       @RequestParam(required = false) Double maxPrice) {
        properties.upsertSkuOverride(sku, cost, currentPrice, competitorPrice, daysInInventory, minPrice, maxPrice);

        // A SKU priced for the first time needs demand history to pass the
        // workflow's validate() step; seed a flat (STABLE-trend) baseline so
        // it's immediately priceable, without overwriting history it already has.
        demandHistoryBySku.computeIfAbsent(sku, PricingInputsController::flatBaselineDemand);

        return effectiveInputs(sku);
    }

    @GetMapping("/{sku}")
    public Map<String, Object> get(@PathVariable String sku) {
        return effectiveInputs(sku);
    }

    private Map<String, Object> effectiveInputs(String sku) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sku", sku);
        result.put("cost", pricingInputsProvider.getCost(sku));
        result.put("currentPrice", pricingInputsProvider.getCurrentPrice(sku));
        result.put("competitorPrice", pricingInputsProvider.getCompetitorPrice(sku));
        result.put("daysInInventory", pricingInputsProvider.getDaysInInventory(sku));
        result.put("minPrice", pricingInputsProvider.getMinPrice(sku));
        result.put("maxPrice", pricingInputsProvider.getMaxPrice(sku));
        return result;
    }

    private static List<DemandDataPoint> flatBaselineDemand(String sku) {
        List<DemandDataPoint> history = new CopyOnWriteArrayList<>();
        LocalDate start = LocalDate.now().minusDays(BASELINE_DAYS - 1);
        for (int day = 0; day < BASELINE_DAYS; day++) {
            history.add(new DemandDataPoint(sku, start.plusDays(day), BASELINE_UNITS));
        }
        return history;
    }
}
