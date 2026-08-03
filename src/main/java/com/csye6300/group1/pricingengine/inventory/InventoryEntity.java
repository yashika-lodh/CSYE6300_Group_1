package com.csye6300.group1.pricingengine.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity backing InventoryRepository. Represents one persisted stock
 * row (sku -> quantity) in the Milestone 3 database, replacing the
 * inventory.csv file used in Milestone 2.
 */
@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    private String sku;

    private int quantity;

    protected InventoryEntity() {
        // required by JPA
    }

    public InventoryEntity(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
