package com.csye6300.group1.pricingengine.channel;

import com.csye6300.group1.pricingengine.shopify.ShopifyApiClient;

/**
 * Factory Method pattern: centralizes creation of SalesChannel implementations
 * so callers (e.g. Main / ChannelManager setup) don't need to know the
 * concrete construction details (API clients, credentials, caching config)
 * for each channel type. Adding a new channel type (own website, social
 * commerce) means adding one case here, not touching client code.
 */
public class SalesChannelFactory {

    public enum ChannelType {
        SHOPIFY,
        OWN_WEBSITE,
        SOCIAL_COMMERCE
    }

    public SalesChannel createChannel(ChannelType type) {
        switch (type) {
            case SHOPIFY:
                return new ShopifyChannel(new ShopifyApiClient());
            case OWN_WEBSITE:
                return new OwnWebsiteChannel();
            case SOCIAL_COMMERCE:
                return new SocialCommerceChannel();
            default:
                throw new IllegalArgumentException("Unsupported channel type: " + type);
        }
    }
}
