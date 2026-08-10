/**
 * inventory-panel.js — renders the Inventory panel: current stock for every
 * SKU the backend already knows about (via GET /api/inventory, which
 * returns the full Inventory singleton snapshot), plus +/- controls to
 * adjust stock. After any adjustment, re-fetches so the panel reflects
 * whatever the backend's Observer-triggered reprice just did.
 */

const InventoryPanel = (() => {
  // Manually-added SKUs not yet known to the backend snapshot (e.g. a brand
  // new SKU with no stock recorded yet). Merged with the live snapshot on render.
  let manuallyTrackedSkus = [];
  let lastKnownSkus = [];

  function getTrackedSkus() {
    return [...lastKnownSkus];
  }

  function trackSku(sku) {
    if (sku && !manuallyTrackedSkus.includes(sku) && !lastKnownSkus.includes(sku)) {
      manuallyTrackedSkus.push(sku);
    }
  }

  function rowHtml(sku, quantity, failed = false) {
    const qtyDisplay = failed ? "—" : quantity;
    return `
      <tr data-sku="${sku}">
        <td>${sku}</td>
        <td class="qty-cell">${qtyDisplay}</td>
        <td>
          <div class="qty-controls">
            <button class="secondary" data-action="decrement" data-sku="${sku}">-5</button>
            <button class="secondary" data-action="increment" data-sku="${sku}">+5</button>
          </div>
        </td>
      </tr>`;
  }

  async function render() {
    const tbody = document.getElementById("inventory-rows");

    let snapshot = {};
    try {
      snapshot = await Api.getAllInventory(); // { sku: quantity, ... }
    } catch (err) {
      console.error("Failed to load inventory snapshot", err);
      Dashboard.showError(`Couldn't load inventory: ${err.message}`);
    }

    const skusFromBackend = Object.keys(snapshot);
    lastKnownSkus = [...new Set([...skusFromBackend, ...manuallyTrackedSkus])];

    if (lastKnownSkus.length === 0) {
      tbody.innerHTML = `<tr class="empty-row"><td colspan="3">No SKUs yet — add one above.</td></tr>`;
      return;
    }

    const rows = lastKnownSkus.map((sku) => rowHtml(sku, snapshot[sku] ?? 0, !(sku in snapshot) && !manuallyTrackedSkus.includes(sku)));
    tbody.innerHTML = rows.join("");

    tbody.querySelectorAll("button[data-action]").forEach((btn) => {
      btn.addEventListener("click", onAdjustClick);
    });
  }

  async function onAdjustClick(event) {
    const btn = event.currentTarget;
    const sku = btn.dataset.sku;
    const delta = btn.dataset.action === "increment" ? 5 : -5;

    btn.disabled = true;
    Dashboard.beginManualAction();
    try {
      await Api.adjustStock(sku, delta);
      // Stock change may have triggered a reprice on the backend (Observer
      // pattern) — refresh both panels so the dashboard reflects it.
      await render();
      if (window.PricingPanel) {
        await window.PricingPanel.render();
      }
    } catch (err) {
      console.error("Failed to adjust stock for", sku, err);
      Dashboard.showError(`Couldn't adjust stock for ${sku}: ${err.message}`);
    } finally {
      btn.disabled = false;
      Dashboard.endManualAction();
    }
  }

  function wireToolbar() {
    const input = document.getElementById("inventory-sku-input");
    const addBtn = document.getElementById("inventory-lookup-btn");

    addBtn.addEventListener("click", async () => {
      const sku = input.value.trim();
      if (!sku) return;
      trackSku(sku);
      input.value = "";
      Dashboard.beginManualAction();
      try {
        await render();
        if (window.PricingPanel) {
          await window.PricingPanel.render();
        }
      } finally {
        Dashboard.endManualAction();
      }
    });

    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter") addBtn.click();
    });
  }

  return { render, wireToolbar, getTrackedSkus, trackSku };
})();

window.InventoryPanel = InventoryPanel;
