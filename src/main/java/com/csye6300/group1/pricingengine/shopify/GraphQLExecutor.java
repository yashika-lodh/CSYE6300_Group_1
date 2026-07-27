package com.csye6300.group1.pricingengine.shopify;

/**
 * Abstraction over "send a GraphQL query/mutation string, get a raw response
 * back". In production this would wrap an HTTP client hitting Shopify's
 * Admin GraphQL API. For Milestone 2 we ship an in-memory
 * MockGraphQLExecutor so unit tests never need real Shopify credentials.
 */
public interface GraphQLExecutor {
    String execute(String query);
}
