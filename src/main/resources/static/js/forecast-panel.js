/**
 * forecast-panel.js — renders the Demand Forecast panel: for every tracked
 * SKU, the demand Trend the Strategy pattern's selector detected and which
 * concrete PricingStrategy it mapped to. Read-only -- calling
 * Api.getForecast never triggers a reprice, it only previews what one would
 * decide. Reuses InventoryPanel's tracked-SKU list, same as PricingPanel.
 */

const ForecastPanel = (() => {

  function rowHtml(sku, trend, strategy, failed = false) {
    if (failed) {
      return `<tr data-sku="${sku}"><td>${sku}</td><td colspan="2">—</td></tr>`;
    }
    return `
      <tr data-sku="${sku}">
        <td>${sku}</td>
        <td><span class="trend-badge ${trend}">${trend}</span></td>
        <td>${strategy}</td>
      </tr>`;
  }

  async function renderRowForSku(sku) {
    try {
      const data = await Api.getForecast(sku);
      return rowHtml(sku, data.trend, data.strategy);
    } catch (err) {
      console.error("Failed to load forecast for", sku, err);
      return rowHtml(sku, null, null, true);
    }
  }

  async function render() {
    const tbody = document.getElementById("forecast-rows");
    const skus = window.InventoryPanel ? window.InventoryPanel.getTrackedSkus() : [];

    if (skus.length === 0) {
      tbody.innerHTML = `<tr class="empty-row"><td colspan="3">No SKUs tracked yet — add one in the Inventory panel.</td></tr>`;
      return;
    }

    const rows = await Promise.all(skus.map(renderRowForSku));
    tbody.innerHTML = rows.join("");
  }

  return { render };
})();

window.ForecastPanel = ForecastPanel;
