/**
 * Panel 4 — Brand-Positioning Guardrails Panel
 */

window.GuardrailsPanelModule = {
  currentPage: 1,
  PAGE_SIZE: 8,

  async refresh() {
    const tableBody = document.getElementById('guardrails-table-body');
    const paginationEl = document.getElementById('guardrails-pagination');
    if (!tableBody) return;

    const trackedSkus = Array.from(window.TrackedSkus);
    if (trackedSkus.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="6" class="state-unavailable">No tracked SKUs available.</td></tr>`;
      if (paginationEl) paginationEl.innerHTML = '';
      return;
    }

    const { pageItems: pagedSkus, currentPage, totalPages, totalItems } =
      window.Paginator.slice(trackedSkus, this.currentPage, this.PAGE_SIZE);
    this.currentPage = currentPage;

    let rowsHtml = '';

    for (const sku of pagedSkus) {
      try {
        // Fetch current pricing & inputs for the SKU
        let currentPrice = null;
        let minPrice = null;
        let maxPrice = null;

        try {
          // Real shape from PricingController: { sku, prices: { "Shopify": 24.30, ... } }
          // -- every channel gets the same price after a reprice, so any one of them
          // represents "the current price" for guardrail comparison purposes.
          const pricingData = await window.API.getPricing(sku);
          if (pricingData && pricingData.prices && typeof pricingData.prices === 'object') {
            const values = Object.values(pricingData.prices);
            if (values.length > 0 && typeof values[0] === 'number') {
              currentPrice = values[0];
            }
          }
        } catch (e) {
          // ignore error
        }

        try {
          // Real persisted values from PricingInputsController, not just whatever this
          // page session happens to have cached -- otherwise a reload always shows blank
          // guardrails even when they're actually configured server-side. 0 means "unset"
          // in this backend's convention (PricingContext.hasMinPrice()/hasMaxPrice()).
          const inputs = await window.API.getPricingInputs(sku);
          minPrice = inputs && inputs.minPrice > 0 ? inputs.minPrice : null;
          maxPrice = inputs && inputs.maxPrice > 0 ? inputs.maxPrice : null;
          if (!window.SkuInputsMap) window.SkuInputsMap = new Map();
          window.SkuInputsMap.set(sku, { ...(window.SkuInputsMap.get(sku) || {}), minPrice, maxPrice });
        } catch (e) {
          // Backend fetch failed -- fall back to whatever's cached from this session
          // rather than showing nothing.
          const storedInputs = window.SkuInputsMap?.get(sku) || {};
          minPrice = storedInputs.minPrice ?? null;
          maxPrice = storedInputs.maxPrice ?? null;
        }

        // Compute client-side guardrail status
        const statusBadge = this.computeGuardrailStatus(currentPrice, minPrice, maxPrice);

        const currentPriceFormatted = currentPrice !== null && !isNaN(currentPrice) ? `$${Number(currentPrice).toFixed(2)}` : '—';
        const minVal = minPrice !== null && !isNaN(minPrice) ? minPrice : '';
        const maxVal = maxPrice !== null && !isNaN(maxPrice) ? maxPrice : '';

        rowsHtml += `
          <tr>
            <td class="code-font">${this.escapeHtml(sku)}</td>
            <td><strong>${currentPriceFormatted}</strong></td>
            <td>
              <input type="number" step="0.01" class="form-input" style="width: 100px; padding: 0.3rem;"
                id="guardrail-min-${this.escapeHtml(sku)}" value="${minVal}" placeholder="Min $">
            </td>
            <td>
              <input type="number" step="0.01" class="form-input" style="width: 100px; padding: 0.3rem;"
                id="guardrail-max-${this.escapeHtml(sku)}" value="${maxVal}" placeholder="Max $">
            </td>
            <td>${statusBadge}</td>
            <td>
              <button class="btn btn-primary btn-sm" onclick="window.GuardrailsPanelModule.saveGuardrails('${this.escapeHtml(sku)}')">
                Save
              </button>
            </td>
          </tr>
        `;
      } catch (err) {
        rowsHtml += `
          <tr>
            <td class="code-font">${this.escapeHtml(sku)}</td>
            <td colspan="5" class="state-unavailable">Guardrail details unavailable</td>
          </tr>
        `;
      }
    }

    tableBody.innerHTML = rowsHtml;

    if (paginationEl) {
      paginationEl.innerHTML = window.Paginator.renderControls({ currentPage, totalPages, totalItems });
      window.Paginator.attach(paginationEl, this);
    }
  },

  computeGuardrailStatus(currentPrice, minPrice, maxPrice) {
    const min = minPrice !== null && !isNaN(minPrice) && minPrice !== '' ? Number(minPrice) : null;
    const max = maxPrice !== null && !isNaN(maxPrice) && maxPrice !== '' ? Number(maxPrice) : null;
    const price = currentPrice !== null && !isNaN(currentPrice) ? Number(currentPrice) : null;

    if (min === null && max === null) {
      return `<span class="badge badge-gray">No guardrail set</span>`;
    }

    if (price === null) {
      return `<span class="badge badge-gray">No price data</span>`;
    }

    if (min !== null && price <= min) {
      return `<span class="badge badge-red">At floor</span>`;
    }

    if (max !== null && price >= max) {
      return `<span class="badge badge-red">At ceiling</span>`;
    }

    return `<span class="badge badge-green">Within range</span>`;
  },

  async saveGuardrails(sku) {
    const minEl = document.getElementById(`guardrail-min-${sku}`);
    const maxEl = document.getElementById(`guardrail-max-${sku}`);

    const minPrice = minEl?.value !== '' ? minEl.value : null;
    const maxPrice = maxEl?.value !== '' ? maxEl.value : null;

    // Cache locally in SkuInputsMap
    if (!window.SkuInputsMap) window.SkuInputsMap = new Map();
    const existing = window.SkuInputsMap.get(sku) || {};
    window.SkuInputsMap.set(sku, { ...existing, minPrice, maxPrice });

    try {
      await window.API.savePricingInputs(sku, { minPrice, maxPrice });
      if (window.refreshAllPanels) window.refreshAllPanels();
    } catch (err) {
      alert(`Failed to save guardrails for ${sku}: ${err.message || 'Error'}`);
    }
  },

  escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
};
