package com.csye6300.group1.pricingengine.channel;

import com.csye6300.group1.pricingengine.inventory.Inventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Concrete SalesChannel for social commerce storefronts (e.g. Instagram/
 * Facebook Shop catalog feeds). No live catalog-feed API client exists yet
 * for this channel type -- that integration is planned for a later milestone
 * -- so prices are kept in memory and publishPrice logs what would be sent,
 * the same bridging approach ShopifyChannel used before ShopifyApiClient
 * existed.
 */
public class SocialCommerceChannel implements SalesChannel {

    private final Inventory inventory = Inventory.getInstance();
    private final Map<String, Double> priceBook = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(SocialCommerceChannel.class.getName());

    @Override
    public String getName() {
        return "SocialCommerce";
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
        logger.fine(() -> "Social-commerce catalog feed would publish " + sku + " at " + price);
    }
}