package com.csye6300.group1.pricingengine.shopify;

public final class ShopifyProduct {
    private final String sku;
    private final double price;
    private final int inventoryQuantity;

    public ShopifyProduct(String sku, double price, int inventoryQuantity) {
        this.sku = sku;
        this.price = price;
        this.inventoryQuantity = inventoryQuantity;
    }

    public String getSku() { return sku; }
    public double getPrice() { return price; }
    public int getInventoryQuantity() { return inventoryQuantity; }
}
