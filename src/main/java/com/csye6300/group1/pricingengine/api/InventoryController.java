package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.inventory.Inventory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * REST facade over the Singleton Inventory. Stock changes made here flow
 * through the same Observer wiring as the console demo, so an adjustment
 * can automatically trigger a reprice via RepricingTriggerObserver.
 *
 * Also feeds real sales into the demand history: a negative delta (a sale)
 * appends a DemandDataPoint for today, so DemandForecaster's moving-average
 * trend can actually shift in response to activity on this dashboard,
 * instead of always reading the same static seed data.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final Inventory inventory;
    private final Map<String, List<DemandDataPoint>> demandHistoryBySku;

    public InventoryController(Inventory inventory,
                                Map<String, List<DemandDataPoint>> demandHistoryBySku) {
        this.inventory = inventory;
        this.demandHistoryBySku = demandHistoryBySku;
    }

    @GetMapping
    public Map<String, Integer> getAllStock() {
        return inventory.snapshot();
    }

    @GetMapping("/{sku}")
    public Map<String, Object> getStock(@PathVariable String sku) {
        return Map.of("sku", sku, "quantity", inventory.getStock(sku));
    }

    /** Sets the absolute stock level for a SKU. */
    @PutMapping("/{sku}")
    public Map<String, Object> setStock(@PathVariable String sku, @RequestParam int quantity) {
        inventory.updateStock(sku, quantity);
        return Map.of("sku", sku, "quantity", inventory.getStock(sku));
    }

    /**
     * Applies a relative delta (positive = restock, negative = sale).
     * A sale (negative delta) also records a DemandDataPoint so the
     * DemandForecaster sees real, evolving demand instead of static seed data --
     * but records the units actually removed from stock, not the raw requested
     * delta. Inventory.adjustStock() clamps at 0, so requesting to sell more
     * than is in stock (e.g. -10 against a stock of 2) only actually sells 2;
     * recording "10 sold" would overstate demand for a sale that didn't happen.
     */
    @PostMapping("/{sku}/adjust")
    public Map<String, Object> adjustStock(@PathVariable String sku, @RequestParam int delta) {
        int stockBefore = inventory.getStock(sku);
        inventory.adjustStock(sku, delta);
        int stockAfter = inventory.getStock(sku);

        if (delta < 0) {
            int unitsActuallySold = stockBefore - stockAfter;
            if (unitsActuallySold > 0) {
                List<DemandDataPoint> history =
                        demandHistoryBySku.computeIfAbsent(sku, s -> new CopyOnWriteArrayList<>());
                history.add(new DemandDataPoint(sku, LocalDate.now(), unitsActuallySold));
            }
        }

        return Map.of("sku", sku, "quantity", stockAfter);
    }
}