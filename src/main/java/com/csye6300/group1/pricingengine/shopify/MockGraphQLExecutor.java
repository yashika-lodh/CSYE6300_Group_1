package com.csye6300.group1.pricingengine.shopify;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates a Shopify Admin GraphQL endpoint entirely in memory, so tests and
 * local development never need a real Shopify store or API credentials.
 * Seed data can be adjusted with putProduct(); real network calls are never made.
 */
public class MockGraphQLExecutor implements GraphQLExecutor {

    private final Map<String, ShopifyProduct> products = new ConcurrentHashMap<>();

    public MockGraphQLExecutor() {
        // seed a couple of demo products so the client works out of the box
        products.put("SKU-1001", new ShopifyProduct("SKU-1001", 24.99, 120));
        products.put("SKU-2002", new ShopifyProduct("SKU-2002", 49.99, 40));
    }

    public void putProduct(ShopifyProduct product) {
        products.put(product.getSku(), product);
    }

    @Override
    public String execute(String query) {
        // Extremely small "query engine": look for the sku the caller embedded
        // in the query string and decide whether it's a query or a mutation.
        if (query.contains("mutation")) {
            return "{\"data\":{\"productVariantUpdate\":{\"userErrors\":[]}}}";
        }
        for (String sku : products.keySet()) {
            if (query.contains(sku)) {
                ShopifyProduct p = products.get(sku);
                return String.format(
                        "{\"data\":{\"product\":{\"sku\":\"%s\",\"price\":%.2f,\"inventoryQuantity\":%d}}}",
                        p.getSku(), p.getPrice(), p.getInventoryQuantity());
            }
        }
        return "{\"data\":{\"product\":null}}";
    }
}
