package com.csye6300.group1.pricingengine.forecast;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemandForecasterTest {

    private List<DemandDataPoint> buildHistory(double baselineUnits, double recentUnits) {
        List<DemandDataPoint> history = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(30);
        for (int day = 0; day < 30; day++) {
            double units = day >= 23 ? recentUnits : baselineUnits;
            history.add(new DemandDataPoint("SKU-1", start.plusDays(day), units));
        }
        return history;
    }

    @Test
    void detectsRisingTrendWhenRecentDemandGrowsByMoreThan10Percent() {
        DemandForecaster forecaster = new DemandForecaster();
        Trend trend = forecaster.forecastTrend(buildHistory(10, 20));
        assertEquals(Trend.RISING, trend);
    }

    @Test
    void detectsFallingTrendWhenRecentDemandDropsByMoreThan10Percent() {
        DemandForecaster forecaster = new DemandForecaster();
        Trend trend = forecaster.forecastTrend(buildHistory(20, 5));
        assertEquals(Trend.FALLING, trend);
    }

    @Test
    void detectsStableTrendWhenDemandBarelyChanges() {
        DemandForecaster forecaster = new DemandForecaster();
        Trend trend = forecaster.forecastTrend(buildHistory(10, 10.5));
        assertEquals(Trend.STABLE, trend);
    }

    @Test
    void emptyHistoryIsTreatedAsStable() {
        DemandForecaster forecaster = new DemandForecaster();
        assertEquals(Trend.STABLE, forecaster.forecastTrend(List.of()));
    }
}
