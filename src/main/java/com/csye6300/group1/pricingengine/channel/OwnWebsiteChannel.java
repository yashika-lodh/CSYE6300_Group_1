package com.csye6300.group1.pricingengine.channel;

import com.csye6300.group1.pricingengine.inventory.Inventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Concrete SalesChannel for the brand's own storefront (headless site / owned
 * checkout). Unlike ShopifyChannel there is no external API to reconcile
 * against: this channel *is* the source of truth for price, so publishPrice
 * simply confirms the in-memory price is live rather than syncing to a
 * third party.
 */
public class OwnWebsiteChannel implements SalesChannel {

    private final Inventory inventory = Inventory.getInstance();
    private final Map<String, Double> priceBook = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(OwnWebsiteChannel.class.getName());

    @Override
    public String getName() {
        return "OwnWebsite";
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void setPrice(String sku, double price) {
        priceBook.put(sku, price);
    }

    @Override
    public double getPrice(String sku) {
        return priceBook.getOrDefault(sku, 0.0);
    }

    @Override
    public void publishPrice(String sku) {
        double price = priceBook.getOrDefault(sku, 0.0);
        logger.fine(() -> "Own-website price live for " + sku + ": " + price);
    }
}