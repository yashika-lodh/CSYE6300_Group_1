package com.csye6300.group1.pricingengine.channel;

import com.csye6300.group1.pricingengine.command.PriceReceiver;
import com.csye6300.group1.pricingengine.inventory.Inventory;

/**
 * Common contract implemented by every channel we sell through (Shopify, own
 * website, social commerce). ChannelManager treats every channel uniformly
 * through this interface. Extends PriceReceiver so PricingCommand can target
 * a channel directly.
 */
public interface SalesChannel extends PriceReceiver {
    String getName();
    Inventory getInventory();

    /** Pushes the current in-memory price for a SKU out to the actual channel (API call, etc.). */
    void publishPrice(String sku);
}
