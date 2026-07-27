package com.csye6300.group1.pricingengine.forecast;

import java.util.List;

/**
 * Lightweight "AI": no external ML library, just a 7-day vs 30-day moving
 * average comparison. Growth of more than 10% is RISING, decay of more than
 * 10% is FALLING, anything in between is STABLE.
 */
public class DemandForecaster {

    private static final int SHORT_WINDOW = 7;
    private static final int LONG_WINDOW = 30;
    private static final double GROWTH_THRESHOLD = 0.10;

    /**
     * @param history demand history sorted oldest -> newest for a single SKU
     */
    public double movingAverage(List<DemandDataPoint> history, int windowSize) {
        if (history.isEmpty()) {
            return 0.0;
        }
        int fromIndex = Math.max(0, history.size() - windowSize);
        List<DemandDataPoint> window = history.subList(fromIndex, history.size());
        return window.stream().mapToDouble(DemandDataPoint::getUnitsSold).average().orElse(0.0);
    }

    public Trend forecastTrend(List<DemandDataPoint> history) {
        double shortAvg = movingAverage(history, SHORT_WINDOW);
        double longAvg = movingAverage(history, LONG_WINDOW);

        if (longAvg == 0.0) {
            return Trend.STABLE;
        }

        double growth = (shortAvg - longAvg) / longAvg;

        if (growth > GROWTH_THRESHOLD) {
            return Trend.RISING;
        } else if (growth < -GROWTH_THRESHOLD) {
            return Trend.FALLING;
        }
        return Trend.STABLE;
    }
}
