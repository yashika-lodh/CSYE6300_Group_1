package com.csye6300.group1.pricingengine.channel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class SalesChannelFactoryTest {

    private final SalesChannelFactory factory = new SalesChannelFactory();

    @Test
    void createsShopifyChannel() {
        SalesChannel channel = factory.createChannel(SalesChannelFactory.ChannelType.SHOPIFY);
        assertInstanceOf(ShopifyChannel.class, channel);
        assertEquals("Shopify", channel.getName());
    }

    @Test
    void createsOwnWebsiteChannel() {
        SalesChannel channel = factory.createChannel(SalesChannelFactory.ChannelType.OWN_WEBSITE);
        assertInstanceOf(OwnWebsiteChannel.class, channel);
        assertEquals("OwnWebsite", channel.getName());
    }

    @Test
    void createsSocialCommerceChannel() {
        SalesChannel channel = factory.createChannel(SalesChannelFactory.ChannelType.SOCIAL_COMMERCE);
        assertInstanceOf(SocialCommerceChannel.class, channel);
        assertEquals("SocialCommerce", channel.getName());
    }

    @Test
    void newChannelsTrackPricePerSkuIndependently() {
        SalesChannel ownWebsite = factory.createChannel(SalesChannelFactory.ChannelType.OWN_WEBSITE);
        ownWebsite.setPrice("SKU-1", 42.0);
        assertEquals(42.0, ownWebsite.getPrice("SKU-1"));
        assertEquals(0.0, ownWebsite.getPrice("SKU-UNSET"));
    }

    @Test
    void newChannelsShareTheSameInventorySingleton() {
        SalesChannel ownWebsite = factory.createChannel(SalesChannelFactory.ChannelType.OWN_WEBSITE);
        SalesChannel socialCommerce = factory.createChannel(SalesChannelFactory.ChannelType.SOCIAL_COMMERCE);
        assertSame(ownWebsite.getInventory(), socialCommerce.getInventory());
    }
}