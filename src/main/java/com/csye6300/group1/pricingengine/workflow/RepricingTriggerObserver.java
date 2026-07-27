package com.csye6300.group1.pricingengine.workflow;

import com.csye6300.group1.pricingengine.inventory.InventoryObserver;
import com.csye6300.group1.pricingengine.inventory.StockChangeEvent;

/**
 * Observer pattern in action: registered with the Inventory singleton so that
 * any stock change (a sale, a restock) automatically triggers a repricing
 * check for that SKU, keeping prices aligned with current inventory levels
 * without any channel having to remember to call reprice() itself.
 */
public class RepricingTriggerObserver implements InventoryObserver {

    private final RepricingWorkflow workflow;

    public RepricingTriggerObserver(RepricingWorkflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public void onStockChanged(StockChangeEvent event) {
        workflow.reprice(event.getSku());
    }
}
