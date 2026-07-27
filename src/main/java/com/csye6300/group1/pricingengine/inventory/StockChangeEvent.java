package com.csye6300.group1.pricingengine.inventory;

/**
 * Immutable event describing a stock level change for a single SKU.
 * Passed to every registered InventoryObserver when Inventory#updateStock is called.
 */
public final class StockChangeEvent {

    private final String sku;
    private final int previousQuantity;
    private final int newQuantity;

    public StockChangeEvent(String sku, int previousQuantity, int newQuantity) {
        this.sku = sku;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
    }

    public String getSku() {
        return sku;
    }

    public int getPreviousQuantity() {
        return previousQuantity;
    }

    public int getNewQuantity() {
        return newQuantity;
    }

    public int getDelta() {
        return newQuantity - previousQuantity;
    }

    @Override
    public String toString() {
        return "StockChangeEvent{sku='" + sku + "', " + previousQuantity + " -> " + newQuantity + "}";
    }
}
