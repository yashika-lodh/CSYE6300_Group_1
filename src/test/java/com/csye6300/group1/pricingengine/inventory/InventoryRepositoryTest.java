package com.csye6300.group1.pricingengine.inventory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the Milestone 3 JPA/DB-backed InventoryRepository, which replaces
 * the inventory.csv file as the durable store for stock levels. Each test
 * run points at its own isolated in-memory H2 database so tests never share
 * state or touch the real ./data/pricingdb file.
 */
class InventoryRepositoryTest {

    private InventoryRepository repository;

    @BeforeEach
    void setUp() {
        String dbName = "inventory-repo-test-" + UUID.randomUUID();
        repository = new InventoryRepository(Map.of(
                "jakarta.persistence.jdbc.url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1",
                "jakarta.persistence.jdbc.driver", "org.h2.Driver"
        ));
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void upsertStockInsertsNewRow() {
        repository.upsertStock("SKU-1001", 42);
        assertEquals(42, repository.findStock("SKU-1001"));
    }

    @Test
    void upsertStockUpdatesExistingRow() {
        repository.upsertStock("SKU-1001", 10);
        repository.upsertStock("SKU-1001", 25);
        assertEquals(25, repository.findStock("SKU-1001"));
    }

    @Test
    void findStockReturnsZeroForUnknownSku() {
        assertEquals(0, repository.findStock("UNKNOWN"));
    }

    @Test
    void findAllReturnsEveryPersistedSku() {
        repository.upsertStock("SKU-1001", 5);
        repository.upsertStock("SKU-2002", 8);

        Map<String, Integer> all = repository.findAll();

        assertEquals(2, all.size());
        assertEquals(5, all.get("SKU-1001"));
        assertEquals(8, all.get("SKU-2002"));
    }

    @Test
    void deleteAllClearsPersistedRows() {
        repository.upsertStock("SKU-1001", 5);
        repository.deleteAll();
        assertTrue(repository.findAll().isEmpty());
    }
}
