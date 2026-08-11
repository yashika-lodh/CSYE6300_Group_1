/**
 * pricing-panel.js — renders the Pricing panel: current price per SKU per
 * channel, and a "Reprice now" button per SKU that calls the repricing
 * endpoint and immediately shows the resulting price. Reuses
 * InventoryPanel's tracked-SKU list so both panels always show the same set
 * of SKUs.
 *
 * Note: PricingController does not expose the demand trend (RISING/FALLING/
 * STABLE) over HTTP, so this panel shows price only, not trend.
 */

const PricingPanel = (() => {

  async function renderRowsForSku(sku) {
    try {
      const data = await Api.getPrices(sku);
      const prices = data.prices || {};
      const channels = Object.keys(prices);

      if (channels.length === 0) {
        return [rowHtml(sku, "—", null, true)];
      }

      return channels.map((channel, idx) =>
        rowHtml(sku, channel, prices[channel], idx === 0, false));
    } catch (err) {
      console.error("Failed to load pricing for", sku, err);
      return [rowHtml(sku, "—", null, true, true)];
    }
  }

  function rowHtml(sku, channel, price, showRepriceButton, failed = false) {
    const priceDisplay = failed ? "—" : (price != null ? `$${Number(price).toFixed(2)}` : "—");
    const actions = showRepriceButton
      ? `<button data-action="reprice" data-sku="${sku}">Reprice now</button>
         <button class="secondary" data-action="undo" data-sku="${sku}">Undo last</button>`
      : "";

    return `
      <tr data-sku="${sku}" data-channel="${channel}">
        <td>${sku}</td>
        <td>${channel}</td>
        <td class="price-cell">${priceDisplay}</td>
        <td>${actions}</td>
      </tr>`;
  }

  async function render() {
    const tbody = document.getElementById("pricing-rows");
    const skus = window.InventoryPanel ? window.InventoryPanel.getTrackedSkus() : [];

    if (skus.length === 0) {
      tbody.innerHTML = `<tr class="empty-row"><td colspan="4">No SKUs tracked yet — add one in the Inventory panel.</td></tr>`;
      return;
    }

    const rowGroups = await Promise.all(skus.map(renderRowsForSku));
    tbody.innerHTML = rowGroups.flat().join("");

    tbody.querySelectorAll("button[data-action='reprice']").forEach((btn) => {
      btn.addEventListener("click", onRepriceClick);
    });
    tbody.querySelectorAll("button[data-action='undo']").forEach((btn) => {
      btn.addEventListener("click", onUndoClick);
    });
  }

  async function onRepriceClick(event) {
    const btn = event.currentTarget;
    const sku = btn.dataset.sku;

    btn.disabled = true;
    btn.textContent = "Repricing…";
    Dashboard.beginManualAction();
    try {
      await Api.reprice(sku);
      await render();
      if (window.AuditPanel) {
        await window.AuditPanel.render();
      }
    } catch (err) {
      console.error("Failed to reprice", sku, err);
      Dashboard.showError(`Couldn't reprice ${sku}: ${err.message}`);
      btn.disabled = false;
      btn.textContent = "Reprice now";
    } finally {
      Dashboard.endManualAction();
    }
  }

  async function onUndoClick(event) {
    const btn = event.currentTarget;
    const sku = btn.dataset.sku;

    btn.disabled = true;
    btn.textContent = "Undoing…";
    Dashboard.beginManualAction();
    try {
      await Api.undo(sku);
      await render();
      if (window.AuditPanel) {
        await window.AuditPanel.render();
      }
    } catch (err) {
      console.error("Failed to undo", sku, err);
      Dashboard.showError(`Couldn't undo ${sku}: ${err.message}`);
      btn.disabled = false;
      btn.textContent = "Undo last";
    } finally {
      Dashboard.endManualAction();
    }
  }

  return { render };
})();

window.PricingPanel = PricingPanel;
