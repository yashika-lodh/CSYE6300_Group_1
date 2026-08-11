/**
 * guardrails-panel.js — the brand-positioning guardrails view/edit panel:
 * shows every tracked SKU's current min/max price bounds (read via
 * GET /api/pricing-inputs/{sku}) next to its current price, with inline
 * inputs to edit min/max and save them (PUT /api/pricing-inputs/{sku},
 * passing only minPrice/maxPrice so every other pricing input is left
 * untouched). Reuses InventoryPanel's tracked-SKU list, same as the other
 * per-SKU panels.
 *
 * The "status" column is a client-side heuristic, not a backend field: a
 * price sitting at (or past) a configured bound is flagged as "At floor" /
 * "At ceiling" rather than "Within range", so it's visually obvious a
 * guardrail is the reason a price didn't move further with demand.
 */

const GuardrailsPanel = (() => {

  const EPSILON = 0.005;

  function statusFor(price, minPrice, maxPrice) {
    if (price == null) return { label: "—", cls: "" };
    if (minPrice > 0 && price <= minPrice + EPSILON) return { label: "At floor", cls: "guardrail-floor" };
    if (maxPrice > 0 && price >= maxPrice - EPSILON) return { label: "At ceiling", cls: "guardrail-ceiling" };
    if (minPrice > 0 || maxPrice > 0) return { label: "Within range", cls: "guardrail-ok" };
    return { label: "No guardrail set", cls: "" };
  }

  function rowHtml(sku, currentPrice, minPrice, maxPrice, failed = false) {
    if (failed) {
      return `<tr data-sku="${sku}"><td>${sku}</td><td colspan="5">—</td></tr>`;
    }
    const priceDisplay = currentPrice != null ? `$${Number(currentPrice).toFixed(2)}` : "—";
    const status = statusFor(currentPrice, minPrice, maxPrice);

    return `
      <tr data-sku="${sku}">
        <td>${sku}</td>
        <td>${priceDisplay}</td>
        <td><input type="number" step="0.01" class="guardrail-input" data-field="min" data-sku="${sku}" value="${minPrice > 0 ? minPrice : ""}" placeholder="unset" /></td>
        <td><input type="number" step="0.01" class="guardrail-input" data-field="max" data-sku="${sku}" value="${maxPrice > 0 ? maxPrice : ""}" placeholder="unset" /></td>
        <td><span class="guardrail-status ${status.cls}">${status.label}</span></td>
        <td><button class="secondary" data-action="save-guardrail" data-sku="${sku}">Save</button></td>
      </tr>`;
  }

  async function renderRowForSku(sku) {
    try {
      const [inputs, priceData] = await Promise.all([
        Api.getPricingInputs(sku),
        Api.getPrices(sku),
      ]);
      const prices = priceData.prices || {};
      const firstPrice = Object.values(prices)[0] ?? null;
      return rowHtml(sku, firstPrice, inputs.minPrice, inputs.maxPrice);
    } catch (err) {
      console.error("Failed to load guardrails for", sku, err);
      return rowHtml(sku, null, 0, 0, true);
    }
  }

  async function render() {
    const tbody = document.getElementById("guardrails-rows");
    const skus = window.InventoryPanel ? window.InventoryPanel.getTrackedSkus() : [];

    if (skus.length === 0) {
      tbody.innerHTML = `<tr class="empty-row"><td colspan="6">No SKUs tracked yet — add one in the Inventory panel.</td></tr>`;
      return;
    }

    const rows = await Promise.all(skus.map(renderRowForSku));
    tbody.innerHTML = rows.join("");

    tbody.querySelectorAll("button[data-action='save-guardrail']").forEach((btn) => {
      btn.addEventListener("click", onSaveClick);
    });
  }

  async function onSaveClick(event) {
    const btn = event.currentTarget;
    const sku = btn.dataset.sku;
    const row = btn.closest("tr");
    const minInput = row.querySelector('input[data-field="min"]');
    const maxInput = row.querySelector('input[data-field="max"]');

    const minPrice = minInput.value.trim() === "" ? undefined : Number(minInput.value);
    const maxPrice = maxInput.value.trim() === "" ? undefined : Number(maxInput.value);

    btn.disabled = true;
    btn.textContent = "Saving…";
    Dashboard.beginManualAction();
    try {
      await Api.upsertPricingInputs(sku, { minPrice, maxPrice });
      await render();
      if (window.PricingPanel) await window.PricingPanel.render();
      if (window.ForecastPanel) await window.ForecastPanel.render();
    } catch (err) {
      console.error("Failed to save guardrails for", sku, err);
      Dashboard.showError(`Couldn't save guardrails for ${sku}: ${err.message}`);
      btn.disabled = false;
      btn.textContent = "Save";
    } finally {
      Dashboard.endManualAction();
    }
  }

  return { render };
})();

window.GuardrailsPanel = GuardrailsPanel;
