package com.csye6300.group1.pricingengine.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Exposes the Repository pattern's DB-backed persistence (InventoryRepository,
 * replacing the Milestone 2 CSV) as a simple status line for the dashboard, so
 * persistence isn't just an invisible implementation detail proven only by a
 * restart test. Persistence is always on in this app -- there's no in-memory-only
 * mode -- and the server start time is captured once, when this class is first
 * loaded (i.e. at application startup), so a restart is visible in the dashboard.
 */
@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private static final Instant SERVER_STARTED_AT = Instant.now();

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "persistenceEnabled", true,
                "serverStartedAt", SERVER_STARTED_AT.toString());
    }
}
