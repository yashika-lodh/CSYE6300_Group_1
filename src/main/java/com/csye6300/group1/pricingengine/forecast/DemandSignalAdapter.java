package com.csye6300.group1.pricingengine.forecast;

import java.time.LocalDate;

/**
 * Adapter pattern: adapts the external RawDemandSignal shape (product code,
 * ISO date string, order count) to the domain model DemandDataPoint (sku,
 * LocalDate, unitsSold) that DemandForecaster understands. This lets us swap
 * in a different upstream feed later without touching DemandForecaster.
 */
public class DemandSignalAdapter {

    public DemandDataPoint adapt(RawDemandSignal raw) {
        String sku = raw.getProductCode();
        LocalDate date = LocalDate.parse(raw.getIsoDate());
        double unitsSold = raw.getOrderCount() * (double) raw.getAvgUnitsPerOrder();
        return new DemandDataPoint(sku, date, unitsSold);
    }
}
