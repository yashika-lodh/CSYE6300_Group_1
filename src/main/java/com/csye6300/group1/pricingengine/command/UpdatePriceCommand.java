package com.csye6300.group1.pricingengine.command;

/**
 * Concrete Command: applies a new price to a SKU on a given receiver (e.g. a
 * SalesChannel) and can restore the original price on undo().
 */
public class UpdatePriceCommand implements PricingCommand {

    private final PriceReceiver receiver;
    private final String sku;
    private final double originalPrice;
    private final double newPrice;

    public UpdatePriceCommand(PriceReceiver receiver, String sku, double newPrice) {
        this.receiver = receiver;
        this.sku = sku;
        this.originalPrice = receiver.getPrice(sku);
        this.newPrice = newPrice;
    }

    @Override
    public void execute() {
        receiver.setPrice(sku, newPrice);
    }

    @Override
    public void undo() {
        receiver.setPrice(sku, originalPrice);
    }

    @Override
    public String getSku() {
        return sku;
    }

    @Override
    public double getOriginalPrice() {
        return originalPrice;
    }

    @Override
    public double getNewPrice() {
        return newPrice;
    }
}
