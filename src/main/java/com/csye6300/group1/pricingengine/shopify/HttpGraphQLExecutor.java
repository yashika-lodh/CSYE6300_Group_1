package com.csye6300.group1.pricingengine.shopify;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Real, non-mocked GraphQLExecutor. Sends the query/mutation string built by
 * ShopifyApiClient to Shopify's Admin GraphQL API over HTTPS, using an
 * access token for auth. Used in place of MockGraphQLExecutor once a real
 * Shopify dev store + credentials are available (Milestone 3).
 *
 * Nothing else in the codebase changes: ShopifyApiClient only depends on the
 * GraphQLExecutor interface, so swapping Mock -> Http is a one-line change
 * (see ShopifyApiClient's default constructor).
 */
public class HttpGraphQLExecutor implements GraphQLExecutor {

    private static final String API_VERSION = "2024-01";

    private final String shopDomain;      // e.g. "your-dev-store.myshopify.com"
    private final String accessToken;     // Shopify Admin API access token
    private final HttpClient httpClient;

    private final Logger logger = Logger.getLogger(HttpGraphQLExecutor.class.getName());

    public HttpGraphQLExecutor(String shopDomain, String accessToken) {
        this(shopDomain, accessToken, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    /** Package-visible constructor so tests can inject a custom HttpClient (e.g. pointed at a local stub). */
    HttpGraphQLExecutor(String shopDomain, String accessToken, HttpClient httpClient) {
        if (shopDomain == null || shopDomain.isBlank()) {
            throw new IllegalArgumentException("shopDomain is required for HttpGraphQLExecutor");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is required for HttpGraphQLExecutor");
        }
        this.shopDomain = shopDomain;
        this.accessToken = accessToken;
        this.httpClient = httpClient;
    }

    @Override
    public String execute(String query) {
        String endpoint = String.format("https://%s/admin/api/%s/graphql.json", shopDomain, API_VERSION);
        String jsonBody = "{\"query\": " + toJsonString(query) + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("X-Shopify-Access-Token", accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ShopifyApiClient.ShopifyApiException(
                        "Shopify API returned HTTP " + response.statusCode() + ": " + response.body(), null);
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warning("HTTP call to Shopify failed: " + e.getMessage());
            throw new ShopifyApiClient.ShopifyApiException("HTTP call to Shopify failed", e);
        }
    }

    /** Minimal, dependency-free JSON string escaping (no library needed for one field). */
    private String toJsonString(String raw) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : raw.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }
}