package com.csye6300.group1.pricingengine.shopify;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Talks to Shopify via GraphQL queries (fetch) and mutations (update price).
 * Wraps a swappable GraphQLExecutor so production code points at the real
 * Shopify Admin API while tests use MockGraphQLExecutor -- no real
 * credentials are ever needed to run the test suite.
 *
 * Includes a 5-minute TTL cache for reads and falls back to the last cached
 * value if a live fetch fails.
 */
public class ShopifyApiClient {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Pattern PRICE_PATTERN = Pattern.compile("\"price\":([0-9.]+)");
    private static final Pattern QTY_PATTERN = Pattern.compile("\"inventoryQuantity\":([0-9]+)");
    private static final Pattern ERROR_PATTERN = Pattern.compile("\"userErrors\":\\[\\s*\\{");

    private final GraphQLExecutor executor;
    private final Logger logger = Logger.getLogger(ShopifyApiClient.class.getName());

    private final Map<String, ShopifyProduct> cache = new ConcurrentHashMap<>();
    private final Map<String, Instant> cacheTimestamps = new ConcurrentHashMap<>();

    public ShopifyApiClient() {
        this(new MockGraphQLExecutor());
    }

    public ShopifyApiClient(GraphQLExecutor executor) {
        this.executor = executor;
    }

    /** Fetches live product data (price + inventory), using the 5-minute cache when fresh. */
    public ShopifyProduct fetchProduct(String sku) {
        Instant lastFetch = cacheTimestamps.get(sku);
        if (lastFetch != null && Duration.between(lastFetch, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cache.get(sku);
        }

        try {
            String query = buildProductQuery(sku);
            String response = executor.execute(query);
            ShopifyProduct product = parseProduct(sku, response);
            cache.put(sku, product);
            cacheTimestamps.put(sku, Instant.now());
            return product;
        } catch (Exception e) {
            logger.warning("fetchProduct failed for " + sku + ", falling back to cache: " + e.getMessage());
            ShopifyProduct cached = cache.get(sku);
            if (cached != null) {
                return cached;
            }
            throw new ShopifyApiException("No cached data available for " + sku, e);
        }
    }

    /** Pushes a new price to Shopify via a GraphQL mutation and refreshes the local cache. */
    public boolean updatePrice(String sku, double newPrice) {
        try {
            String mutation = buildPriceMutation(sku, newPrice);
            String response = executor.execute(mutation);
            boolean hasErrors = ERROR_PATTERN.matcher(response).find();
            if (!hasErrors) {
                ShopifyProduct existing = cache.get(sku);
                int qty = existing != null ? existing.getInventoryQuantity() : 0;
                cache.put(sku, new ShopifyProduct(sku, newPrice, qty));
                cacheTimestamps.put(sku, Instant.now());
            }
            return !hasErrors;
        } catch (Exception e) {
            logger.warning("updatePrice failed for " + sku + ": " + e.getMessage());
            return false;
        }
    }

    private String buildProductQuery(String sku) {
        return "query { product(sku: \"" + sku + "\") { sku price inventoryQuantity } }";
    }

    private String buildPriceMutation(String sku, double newPrice) {
        return "mutation { productVariantUpdate(sku: \"" + sku + "\", price: \"" + newPrice + "\") { userErrors { field message } } }";
    }

    private ShopifyProduct parseProduct(String sku, String response) {
        Matcher priceMatcher = PRICE_PATTERN.matcher(response);
        Matcher qtyMatcher = QTY_PATTERN.matcher(response);
        if (!priceMatcher.find() || !qtyMatcher.find()) {
            throw new ShopifyApiException("Malformed response for " + sku + ": " + response, null);
        }
        double price = Double.parseDouble(priceMatcher.group(1));
        int qty = Integer.parseInt(qtyMatcher.group(1));
        return new ShopifyProduct(sku, price, qty);
    }

    public static class ShopifyApiException extends RuntimeException {
        public ShopifyApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
