package com.csye6300.group1.pricingengine.forecast;

/**
 * Represents demand data in the "external" shape it arrives in -- e.g. a row
 * parsed from a CSV feed with a different field layout than our domain model
 * (String date, product code instead of sku, order count instead of units sold).
 * DemandSignalAdapter converts this into a DemandDataPoint.
 */
public final class RawDemandSignal {

    private final String productCode;
    private final String isoDate; // "yyyy-MM-dd"
    private final int orderCount;
    private final int avgUnitsPerOrder;

    public RawDemandSignal(String productCode, String isoDate, int orderCount, int avgUnitsPerOrder) {
        this.productCode = productCode;
        this.isoDate = isoDate;
        this.orderCount = orderCount;
        this.avgUnitsPerOrder = avgUnitsPerOrder;
    }

    public String getProductCode() { return productCode; }
    public String getIsoDate() { return isoDate; }
    public int getOrderCount() { return orderCount; }
    public int getAvgUnitsPerOrder() { return avgUnitsPerOrder; }
}
