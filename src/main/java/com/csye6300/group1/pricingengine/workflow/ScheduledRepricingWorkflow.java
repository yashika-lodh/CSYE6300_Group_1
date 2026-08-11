package com.csye6300.group1.pricingengine.workflow;

import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.command.PricingCommandInvoker;
import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.forecast.DemandForecaster;
import com.csye6300.group1.pricingengine.selector.PricingStrategySelector;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Template Method pattern: second concrete workflow in the RepricingWorkflow
 * hierarchy. It reuses every fetch/validate/execute/log step from
 * StandardRepricingWorkflow unchanged, but instead of repricing only in
 * reaction to an Observer stock-change notification, it also fires
 * reprice() on a fixed schedule for a configured set of SKUs - e.g. a
 * nightly repricing sweep that runs whether or not stock moved that day.
 */
public class ScheduledRepricingWorkflow extends StandardRepricingWorkflow {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "scheduled-repricing-workflow");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> scheduledTask;
    private volatile Collection<String> scheduledSkus = Set.of();
    private volatile Instant lastRunAt;

    public ScheduledRepricingWorkflow(Map<String, List<DemandDataPoint>> demandHistoryBySku,
                                       PricingInputsProvider inputsProvider,
                                       DemandForecaster forecaster,
                                       PricingStrategySelector strategySelector,
                                       ChannelManager channelManager,
                                       PricingCommandInvoker invoker) {
        super(demandHistoryBySku, inputsProvider, forecaster, strategySelector, channelManager, invoker);
    }

    /** Starts a repeating reprice sweep across the given SKUs, every `period` (in `unit`). */
    public void startSchedule(Collection<String> skus, long initialDelay, long period, TimeUnit unit) {
        if (scheduledTask != null) {
            throw new IllegalStateException("Schedule already started");
        }
        scheduledSkus = skus;
        scheduledTask = scheduler.scheduleAtFixedRate(
                () -> {
                    lastRunAt = Instant.now();
                    skus.forEach(this::repriceSafely);
                },
                initialDelay,
                period,
                unit);
    }

    /** Cancels the schedule and shuts down the backing executor. */
    public void stopSchedule() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        scheduler.shutdown();
    }

    /** Whether the periodic sweep is currently running (started and not since cancelled). */
    public boolean isActive() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }

    /** The SKUs this schedule sweeps every run. */
    public Collection<String> getScheduledSkus() {
        return scheduledSkus;
    }

    /** When the sweep last fired, or null if it hasn't run yet. */
    public Instant getLastRunAt() {
        return lastRunAt;
    }

    /**
     * When the sweep is next due to fire, computed from the executor's own remaining delay
     * (rather than re-deriving it from lastRunAt + period) so it stays accurate even if a run
     * took a while or the schedule hasn't fired for the first time yet.
     */
    public Instant getNextRunAt() {
        if (!isActive()) {
            return null;
        }
        return Instant.now().plusMillis(scheduledTask.getDelay(TimeUnit.MILLISECONDS));
    }

    private void repriceSafely(String sku) {
        try {
            reprice(sku, "SCHEDULED");
        } catch (RuntimeException e) {
            logger.warning("Scheduled reprice failed for " + sku + ": " + e.getMessage());
        }
    }
}
