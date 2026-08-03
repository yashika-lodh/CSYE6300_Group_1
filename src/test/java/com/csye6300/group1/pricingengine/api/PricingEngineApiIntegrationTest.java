package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test that boots the real Spring context (Singleton Inventory,
 * Factory-built Shopify channel, Strategy/Selector, Command/Decorator,
 * Template Method workflow) and drives it purely through the REST layer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PricingEngineApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void tearDown() {
        Inventory.getInstance().resetForTesting();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void inventoryEndpointReportsStockSetOnStartup() {
        ResponseEntity<Map> response = restTemplate.getForEntity(url("/api/inventory/SKU-1001"), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SKU-1001", response.getBody().get("sku"));
    }

    @Test
    void adjustingStockTriggersAnAutomaticReprice() {
        restTemplate.postForEntity(url("/api/inventory/SKU-1001/adjust?delta=-5"), null, Map.class);

        ResponseEntity<Map> priceResponse = restTemplate.getForEntity(url("/api/pricing/SKU-1001"), Map.class);
        Map<String, Object> prices = (Map<String, Object>) priceResponse.getBody().get("prices");

        assertTrue(prices.containsKey("Shopify"));
    }

    @Test
    void manualRepriceEndpointReturnsAPricePerChannel() {
        ResponseEntity<Map> response =
                restTemplate.postForEntity(url("/api/pricing/SKU-1001/reprice"), null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> prices = (Map<String, Object>) response.getBody().get("prices");
        assertTrue(prices.containsKey("Shopify"));
    }

    @Test
    void auditReportIncludesEntriesForARepricedSku() {
        restTemplate.postForEntity(url("/api/pricing/SKU-1001/reprice"), null, Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity(url("/api/audit/SKU-1001"), List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().stream()
                .anyMatch(entry -> entry.toString().contains("sku=SKU-1001")));
    }
}
