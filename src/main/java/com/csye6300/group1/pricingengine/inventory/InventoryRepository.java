package com.csye6300.group1.pricingengine.inventory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * JPA/Hibernate-backed persistence for stock levels, replacing the
 * inventory.csv file used as the interim data source in Milestone 2.
 *
 * The Inventory singleton still holds the fast, in-memory working copy of
 * stock (see Inventory#loadFromRepository); this repository is what
 * actually survives an application restart. Inventory itself never
 * references JPA directly - it is bridged through InventoryPersistenceObserver
 * so the Singleton/Observer design from Milestone 2 is unchanged.
 */
public class InventoryRepository {

    private static final String PERSISTENCE_UNIT = "pricing-engine-pu";

    private final EntityManagerFactory emf;

    /** Reads DB connection settings from application.properties on the classpath. */
    public InventoryRepository() {
        this(loadConfig());
    }

    /** Lets callers (e.g. tests) override persistence-unit properties, such as pointing at an isolated in-memory DB. */
    public InventoryRepository(Map<String, String> overrides) {
        this.emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, overrides);
    }

    private static Map<String, String> loadConfig() {
        Properties props = new Properties();
        try (InputStream in = InventoryRepository.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read application.properties", e);
        }

        Map<String, String> overrides = new HashMap<>();
        putIfPresent(overrides, props, "db.url", "jakarta.persistence.jdbc.url");
        putIfPresent(overrides, props, "db.driver", "jakarta.persistence.jdbc.driver");
        putIfPresent(overrides, props, "db.user", "jakarta.persistence.jdbc.user");
        putIfPresent(overrides, props, "db.password", "jakarta.persistence.jdbc.password");
        return overrides;
    }

    private static void putIfPresent(Map<String, String> target, Properties source, String key, String jpaKey) {
        String value = source.getProperty(key);
        if (value != null) {
            target.put(jpaKey, value);
        }
    }

    /** Inserts or updates the persisted stock row for a SKU. */
    public void upsertStock(String sku, int quantity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            InventoryEntity entity = em.find(InventoryEntity.class, sku);
            if (entity == null) {
                em.persist(new InventoryEntity(sku, quantity));
            } else {
                entity.setQuantity(quantity);
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /** Returns the persisted stock for a SKU, or 0 if it has never been recorded. */
    public int findStock(String sku) {
        EntityManager em = emf.createEntityManager();
        try {
            InventoryEntity entity = em.find(InventoryEntity.class, sku);
            return entity == null ? 0 : entity.getQuantity();
        } finally {
            em.close();
        }
    }

    /** Loads every persisted SKU -> quantity row, used to seed Inventory at startup. */
    public Map<String, Integer> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            Map<String, Integer> result = new LinkedHashMap<>();
            for (InventoryEntity entity :
                    em.createQuery("SELECT e FROM InventoryEntity e", InventoryEntity.class).getResultList()) {
                result.put(entity.getSku(), entity.getQuantity());
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** Test-only hook to clear persisted rows between test runs. */
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM InventoryEntity").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public void close() {
        emf.close();
    }
}
