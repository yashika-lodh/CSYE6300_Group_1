package com.csye6300.group1.pricingengine.workflow;

import com.csye6300.group1.pricingengine.audit.AuditLoggingCommandDecorator;
import com.csye6300.group1.pricingengine.channel.ChannelManager;
import com.csye6300.group1.pricingengine.channel.SalesChannel;
import com.csye6300.group1.pricingengine.command.PricingCommand;
import com.csye6300.group1.pricingengine.command.PricingCommandInvoker;
import com.csye6300.group1.pricingengine.command.UpdatePriceCommand;
import com.csye6300.group1.pricingengine.forecast.DemandDataPoint;
import com.csye6300.group1.pricingengine.forecast.DemandForecaster;
import com.csye6300.group1.pricingengine.forecast.Trend;
import com.csye6300.group1.pricingengine.inventory.Inventory;
import com.csye6300.group1.pricingengine.pricing.PricingContext;
import com.csye6300.group1.pricingengine.pricing.PricingStrategy;
import com.csye6300.group1.pricingengine.selector.PricingStrategySelector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Concrete Template Method implementation that ties every other pattern
 * together end to end:
 *   fetch    -> DemandForecaster reads demand history
 *   validate -> RepricingWorkflow's default hook
 *   execute  -> PricingStrategySelector picks a Strategy, an UpdatePriceCommand
 *               (wrapped by AuditLoggingCommandDecorator) applies it across
 *               every channel in ChannelManager, so every channel's price
 *               change gets its own compliance-log entry
 *   log      -> handled by the Decorator + base class logging
 */
public class StandardRepricingWorkflow extends RepricingWorkflow {

    private final Map<String, List<DemandDataPoint>> demandHistoryBySku;
    private final PricingInputsProvider inputsProvider;
    private final DemandForecaster forecaster;
    private final PricingStrategySelector strategySelector;
    private final ChannelManager channelManager;
    private final PricingCommandInvoker invoker;
    private final Inventory inventory = Inventory.getInstance();

    public StandardRepricingWorkflow(Map<String, List<DemandDataPoint>> demandHistoryBySku,
                                      PricingInputsProvider inputsProvider,
                                      DemandForecaster forecaster,
                                      PricingStrategySelector strategySelector,
                                      ChannelManager channelManager,
                                      PricingCommandInvoker invoker) {
        this.demandHistoryBySku = demandHistoryBySku;
        this.inputsProvider = inputsProvider;
        this.forecaster = forecaster;
        this.strategySelector = strategySelector;
        this.channelManager = channelManager;
        this.invoker = invoker;
    }

    @Override
    protected List<DemandDataPoint> fetchDemandHistory(String sku) {
        return demandHistoryBySku.getOrDefault(sku, List.of());
    }

    @Override
    protected PricingContext buildPricingContext(String sku, List<DemandDataPoint> demandHistory) {
        return new PricingContext(
                sku,
                inputsProvider.getCost(sku),
                inputsProvider.getCurrentPrice(sku),
                inputsProvider.getCompetitorPrice(sku),
                inputsProvider.getDaysInInventory(sku),
                inventory.getStock(sku),
                inputsProvider.getMinPrice(sku),
                inputsProvider.getMaxPrice(sku));
    }

    @Override
    protected ForecastInsight computeForecastInsight(List<DemandDataPoint> demandHistory) {
        Trend trend = forecaster.forecastTrend(demandHistory);
        PricingStrategy strategy = strategySelector.selectStrategy(trend);
        return new ForecastInsight(trend.name(), strategy.getName());
    }

    @Override
    protected double execute(String sku, PricingContext context, List<DemandDataPoint> demandHistory) {
        Trend trend = forecaster.forecastTrend(demandHistory);
        PricingStrategy strategy = strategySelector.selectStrategy(trend);
        double newPrice = strategy.calculatePrice(context);

        // Every channel's command is bundled into one batch, run atomically via
        // executeCommands() -- so a single Undo reverses this whole reprice (every
        // channel), not just one channel's price, regardless of where in the shared
        // multi-SKU undo stack this batch ends up sitting.
        List<PricingCommand> commands = new ArrayList<>();
        for (SalesChannel channel : channelManager.getChannels()) {
            commands.add(new AuditLoggingCommandDecorator(new UpdatePriceCommand(channel, sku, newPrice), currentSource()));
        }
        invoker.executeCommands(commands);
        for (SalesChannel channel : channelManager.getChannels()) {
            channel.publishPrice(sku);
        }

        return newPrice;
    }
}