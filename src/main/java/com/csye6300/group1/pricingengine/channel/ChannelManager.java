package com.csye6300.group1.pricingengine.channel;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds every registered SalesChannel and fans out price updates so all
 * channels stay in sync. Works against the SalesChannel interface only, so
 * new channel types (social commerce, own website) can be added with no
 * changes here.
 */
public class ChannelManager {

    private final List<SalesChannel> channels = new ArrayList<>();

    public void registerChannel(SalesChannel channel) {
        channels.add(channel);
    }

    public List<SalesChannel> getChannels() {
        return List.copyOf(channels);
    }

    /** Applies the given price to the SKU on every registered channel and publishes it. */
    public void updatePricesAcrossChannels(String sku, double price) {
        for (SalesChannel channel : channels) {
            channel.setPrice(sku, price);
            channel.publishPrice(sku);
        }
    }
}
