package com.csye6300.group1.pricingengine.shopify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real network integration test against a Shopify dev store using
 * HttpGraphQLExecutor. Disabled by default (and in CI) since it needs real
 * credentials -- set SHOPIFY_SHOP_DOMAIN and SHOPIFY_ACCESS_TOKEN as
 * environment variables to run it locally against your own dev store.
 *
 * This is intentionally kept separate from the unit test suite (which uses
 * MockGraphQLExecutor and never touches the network) so `mvn test` stays
 * fast and credential-free for every teammate and for grading.
 */
@EnabledIfEnvironmentVariable(named = "SHOPIFY_ACCESS_TOKEN", matches = ".+")
class ShopifyApiClientIntegrationTest {

    @Test
    void fetchProduct_returnsLiveDataFromDevStore() {
        String shopDomain = System.getenv("SHOPIFY_SHOP_DOMAIN");
        String accessToken = System.getenv("SHOPIFY_ACCESS_TOKEN");
        String testSku = System.getenv().getOrDefault("SHOPIFY_TEST_SKU", "SKU-1001");

        ShopifyApiClient client = new ShopifyApiClient(new HttpGraphQLExecutor(shopDomain, accessToken));

        ShopifyProduct product = client.fetchProduct(testSku);

        assertNotNull(product);
        assertTrue(product.getPrice() >= 0.0);
    }
}