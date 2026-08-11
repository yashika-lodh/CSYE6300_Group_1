package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.forecast.DemandForecaster;
import com.csye6300.group1.pricingengine.forecast.Trend;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lets a SKU's demand history grow over time instead of being fixed at
 * startup, so the Strategy the engine picks (via DemandForecaster's 7-day vs
 * 30-day trend) can actually shift from STABLE to RISING/FALLING as new
 * demand is recorded, instead of being permanently whatever the seed CSV
 * happened to contain.
 */
@RestController
@RequestMapping("/api/demand")
public class DemandController {

    private final Map<String, List<DemandDataPoint>> demandHistoryBySku;
    private final DemandForecaster forecaster = new DemandForecaster();

    public DemandController(Map<String, List<DemandDataPoint>> demandHistoryBySku) {
        this.demandHistoryBySku = demandHistoryBySku;
    }

    /** Appends one day of demand for a SKU. Defaults to today if no date is given. */
    @PostMapping("/{sku}")
    public Map<String, Object> recordDemand(@PathVariable String sku,
                                             @RequestParam double units,
                                             @RequestParam(required = false) String date) {
        LocalDate day = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<DemandDataPoint> history =
                demandHistoryBySku.computeIfAbsent(sku, k -> new CopyOnWriteArrayList<>());
        history.add(new DemandDataPoint(sku, day, units));
        return summary(sku, history);
    }

    /** Returns the full recorded history for a SKU plus the trend it currently forecasts. */
    @GetMapping("/{sku}")
    public Map<String, Object> getDemand(@PathVariable String sku) {
        return summary(sku, demandHistoryBySku.getOrDefault(sku, List.of()));
    }

    private Map<String, Object> summary(String sku, List<DemandDataPoint> history) {
        Trend trend = forecaster.forecastTrend(history);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sku", sku);
        result.put("trend", trend.name());
        result.put("points", history.stream()
                .map(p -> Map.of("date", p.getDate().toString(), "units", p.getUnitsSold()))
                .toList());
        return result;
    }
}
