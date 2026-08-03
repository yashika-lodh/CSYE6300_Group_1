package com.csye6300.group1.pricingengine.forecast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reads data/demand_signals.csv (product_code,date,order_count,avg_units_per_order)
 * and converts each row into a DemandDataPoint via DemandSignalAdapter, grouped
 * by SKU. This replaces the hardcoded sample demand history used in Main /
 * PricingEngineApplication for Milestone 2 -- the CSV file existed since
 * Milestone 2 but nothing actually read it until now.
 */
public class DemandSignalRepository {

    private static final Path DEFAULT_CSV_PATH = Path.of("data", "demand_signals.csv");

    private final Path csvPath;
    private final DemandSignalAdapter adapter = new DemandSignalAdapter();
    private final Logger logger = Logger.getLogger(DemandSignalRepository.class.getName());

    public DemandSignalRepository() {
        this(DEFAULT_CSV_PATH);
    }

    public DemandSignalRepository(Path csvPath) {
        this.csvPath = csvPath;
    }

    /** Loads every row in the CSV, converts it, and groups the resulting DemandDataPoints by SKU. */
    public Map<String, List<DemandDataPoint>> loadDemandHistoryBySku() {
        Map<String, List<DemandDataPoint>> bySku = new HashMap<>();

        if (!Files.exists(csvPath)) {
            logger.warning("Demand signals CSV not found at " + csvPath.toAbsolutePath()
                    + " -- returning empty demand history.");
            return bySku;
        }

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String line = reader.readLine(); // header row: product_code,date,order_count,avg_units_per_order
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                RawDemandSignal raw = parseLine(line);
                if (raw == null) {
                    continue;
                }
                DemandDataPoint point = adapter.adapt(raw);
                bySku.computeIfAbsent(point.getSku(), k -> new ArrayList<>()).add(point);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read demand signals CSV at " + csvPath, e);
        }

        return bySku;
    }

    private RawDemandSignal parseLine(String line) {
        String[] fields = line.split(",");
        if (fields.length != 4) {
            logger.warning("Skipping malformed demand signal row: '" + line + "'");
            return null;
        }
        try {
            String productCode = fields[0].trim();
            String isoDate = fields[1].trim();
            int orderCount = Integer.parseInt(fields[2].trim());
            int avgUnitsPerOrder = Integer.parseInt(fields[3].trim());
            return new RawDemandSignal(productCode, isoDate, orderCount, avgUnitsPerOrder);
        } catch (NumberFormatException e) {
            logger.warning("Skipping demand signal row with bad numbers: '" + line + "'");
            return null;
        }
    }
}