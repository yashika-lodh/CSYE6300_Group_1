package com.csye6300.group1.pricingengine.command;

/**
 * Command pattern: encapsulates a single price-change request so it can be
 * queued, logged, executed, and undone uniformly by PricingCommandInvoker,
 * regardless of what kind of change it represents.
 */
public interface PricingCommand {
    void execute();
    void undo();

    String getSku();
    double getOriginalPrice();
    double getNewPrice();
}
