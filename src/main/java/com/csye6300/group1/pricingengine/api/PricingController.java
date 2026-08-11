package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.channel.SalesChannel;
import com.csye6300.group1.pricingengine.workflow.ForecastInsight;
import com.csye6300.group1.pricingengine.workflow.RepricingWorkflow;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST facade over the Template Method RepricingWorkflow. Triggering a
 * reprice here runs the exact same fetch -> validate -> execute -> log
 * pipeline (Strategy selection, Command + Decorator execution) as the
 * Observer-triggered path, just invoked on demand instead of on a stock event.
 */
@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final RepricingWorkflow repricingWorkflow;
    private final ChannelManager channelManager;

    public PricingController(RepricingWorkflow repricingWorkflow, ChannelManager channelManager) {
        this.repricingWorkflow = repricingWorkflow;
        this.channelManager = channelManager;
    }

    /** Runs a full repricing cycle for the SKU and returns the resulting per-channel prices. */
    @PostMapping("/{sku}/reprice")
    public Map<String, Object> reprice(@PathVariable String sku) {
        repricingWorkflow.reprice(sku);
        return Map.of("sku", sku, "prices", pricesByChannel(sku));
    }

    /** Returns the current price for a SKU on every registered sales channel. */
    @GetMapping("/{sku}")
    public Map<String, Object> currentPrices(@PathVariable String sku) {
        return Map.of("sku", sku, "prices", pricesByChannel(sku));
    }

    /**
     * Read-only preview of the detected demand Trend and the PricingStrategy
     * it maps to, without triggering an actual reprice. Surfaces the
     * Strategy pattern's decision for the dashboard's Demand Forecast panel.
     */
    @GetMapping("/{sku}/forecast")
    public Map<String, Object> forecast(@PathVariable String sku) {
        ForecastInsight insight = repricingWorkflow.previewForecast(sku);
        return Map.of("sku", sku, "trend", insight.trend(), "strategy", insight.strategyName());
    }

    private Map<String, Double> pricesByChannel(String sku) {
        Map<String, Double> prices = new LinkedHashMap<>();
        for (SalesChannel channel : channelManager.getChannels()) {
            prices.put(channel.getName(), channel.getPrice(sku));
        }
        return prices;
    }
}
