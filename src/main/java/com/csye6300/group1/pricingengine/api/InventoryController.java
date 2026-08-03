package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.inventory.Inventory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST facade over the Singleton Inventory. Stock changes made here flow
 * through the same Observer wiring as the console demo, so an adjustment
 * can automatically trigger a reprice via RepricingTriggerObserver.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final Inventory inventory;

    public InventoryController(Inventory inventory) {
        this.inventory = inventory;
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

    /** Applies a relative delta (positive = restock, negative = sale). */
    @PostMapping("/{sku}/adjust")
    public Map<String, Object> adjustStock(@PathVariable String sku, @RequestParam int delta) {
        inventory.adjustStock(sku, delta);
        return Map.of("sku", sku, "quantity", inventory.getStock(sku));
    }
}
