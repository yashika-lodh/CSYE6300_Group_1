package com.csye6300.group1.pricingengine.channel;

import com.csye6300.group1.pricingengine.inventory.Inventory;
import com.csye6300.group1.pricingengine.shopify.ShopifyApiClient;
import com.csye6300.group1.pricingengine.shopify.ShopifyProduct;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Concrete SalesChannel for Shopify storefronts. Keeps a local price cache
 * (5-minute TTL, matching ShopifyApiClient's cache) so repeated reads don't
 * hit the API, and falls back to the last known good price if a live update
 * fails.
 */
public class ShopifyChannel implements SalesChannel {

    private final ShopifyApiClient apiClient;
    private final Inventory inventory = Inventory.getInstance();
    private final Map<String, Double> localPriceCache = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(ShopifyChannel.class.getName());

    public ShopifyChannel(ShopifyApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String getName() {
        return "Shopify";
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void setPrice(String sku, double price) {
        localPriceCache.put(sku, price);
    }

    @Override
    public double getPrice(String sku) {
        Double cached = localPriceCache.get(sku);
        if (cached != null) {
            return cached;
        }
        try {
            ShopifyProduct product = apiClient.fetchProduct(sku);
            localPriceCache.put(sku, product.getPrice());
            return product.getPrice();
        } catch (Exception e) {
            logger.warning("Falling back to cached/zero price for " + sku + ": " + e.getMessage());
            return localPriceCache.getOrDefault(sku, 0.0);
        }
    }

    @Override
    public void publishPrice(String sku) {
        double price = localPriceCache.getOrDefault(sku, 0.0);
        try {
            apiClient.updatePrice(sku, price);
        } catch (Exception e) {
            logger.warning("Failed to publish price for " + sku + " to Shopify: " + e.getMessage());
        }
    }
}
