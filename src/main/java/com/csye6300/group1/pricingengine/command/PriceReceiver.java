package com.csye6300.group1.pricingengine.command;

/**
 * Command pattern "receiver": the object that actually knows how to apply a
 * price change (e.g. a SalesChannel). Kept as a tiny interface so
 * UpdatePriceCommand doesn't need to depend on the channel package directly.
 */
public interface PriceReceiver {
    void setPrice(String sku, double price);
    double getPrice(String sku);
}
