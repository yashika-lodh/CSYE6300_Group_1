package com.csye6300.group1.pricingengine.forecast;

import java.time.LocalDate;

/**
 * Domain model for a single day of demand for one SKU, used internally by
 * DemandForecaster. External/raw signals are converted into this shape by
 * DemandSignalAdapter.
 */
public final class DemandDataPoint {

    private final String sku;
    private final LocalDate date;
    private final double unitsSold;

    public DemandDataPoint(String sku, LocalDate date, double unitsSold) {
        this.sku = sku;
        this.date = date;
        this.unitsSold = unitsSold;
    }

    public String getSku() { return sku; }
    public LocalDate getDate() { return date; }
    public double getUnitsSold() { return unitsSold; }
}
