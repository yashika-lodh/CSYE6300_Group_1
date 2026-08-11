package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.channel.SalesChannel;
import com.csye6300.group1.pricingengine.command.PricingCommandInvoker;
import com.csye6300.group1.pricingengine.workflow.ForecastInsight;
import com.csye6300.group1.pricingengine.workflow.RepricingWorkflow;
import com.csye6300.group1.pricingengine.workflow.ScheduledRepricingWorkflow;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
    private final PricingCommandInvoker invoker;
    private final ScheduledRepricingWorkflow scheduledRepricingWorkflow;

    public PricingController(RepricingWorkflow repricingWorkflow, ChannelManager channelManager,
                              PricingCommandInvoker invoker, ScheduledRepricingWorkflow scheduledRepricingWorkflow) {
        this.repricingWorkflow = repricingWorkflow;
        this.channelManager = channelManager;
        this.invoker = invoker;
        this.scheduledRepricingWorkflow = scheduledRepricingWorkflow;
    }

    /** Runs a full repricing cycle for the SKU and returns the resulting per-channel prices. */
    @PostMapping("/{sku}/reprice")
    public Map<String, Object> reprice(@PathVariable String sku) {
        repricingWorkflow.reprice(sku, "MANUAL");
        return Map.of("sku", sku, "prices", pricesByChannel(sku));
    }

    /**
     * Undoes the most recent reprice for this SKU (Command pattern: PricingCommandInvoker's
     * undo stack). The stack is invoker-wide, not per-SKU -- other SKUs' reprices (from the
     * Observer, the scheduled sweep, or another manual click) can land on top of this SKU's own
     * commands in between a Reprice and an Undo click, so PricingCommandInvoker.undoLastForSku()
     * searches the whole stack for this SKU's most recent batch rather than only checking the
     * top. Returns 409 if this SKU has no undo history anywhere in the stack.
     */
    @PostMapping("/{sku}/undo")
    public ResponseEntity<Map<String, Object>> undo(@PathVariable String sku) {
        if (!invoker.undoLastForSku(sku)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("sku", sku, "message", "Nothing to undo for " + sku + "."));
        }
        return ResponseEntity.ok(Map.of("sku", sku, "prices", pricesByChannel(sku)));
    }

    /** Returns the current price for a SKU on every registered sales channel. */
    @GetMapping("/{sku}")
    public Map<String, Object> currentPrices(@PathVariable String sku) {
        return Map.of("sku", sku, "prices", pricesByChannel(sku));
    }

    /**
     * Surfaces ScheduledRepricingWorkflow's periodic sweep -- the automatic-repricing-on-a-timer
     * side of the Observer/Template Method design that otherwise runs invisibly in the background.
     */
    @GetMapping("/scheduled-status")
    public Map<String, Object> scheduledStatus() {
        Instant lastRunAt = scheduledRepricingWorkflow.getLastRunAt();
        Instant nextRunAt = scheduledRepricingWorkflow.getNextRunAt();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("active", scheduledRepricingWorkflow.isActive());
        response.put("skus", scheduledRepricingWorkflow.getScheduledSkus());
        response.put("lastRunAt", lastRunAt != null ? lastRunAt.toString() : null);
        response.put("nextRunAt", nextRunAt != null ? nextRunAt.toString() : null);
        return response;
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
