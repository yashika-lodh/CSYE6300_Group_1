/**
 * Panel 2 — Inventory Panel
 */

window.InventoryPanelModule = {
  currentPage: 1,
  PAGE_SIZE: 5,

  init() {
    const addSkuBtn = document.getElementById('btn-add-track-sku');
    if (addSkuBtn) {
      addSkuBtn.addEventListener('click', () => this.handleAddTrackSku());
    }
  },

  async refresh() {
    const tableBody = document.getElementById('inventory-table-body');
    const paginationEl = document.getElementById('inventory-pagination');

    if (!tableBody) return;

    try {
      const data = await window.API.getInventory();

      // Parse inventory items
      let items = [];
      if (Array.isArray(data)) {
        items = data;
      } else if (data && typeof data === 'object') {
        // If key-value map like {"SKU-101": 50}
        items = Object.entries(data).map(([sku, stock]) => ({ sku, stock }));
      }

      // Add returned SKUs to tracked SKUs
      items.forEach(item => window.TrackedSkus.add(item.sku));

      // Filter or ensure all tracked SKUs are in the inventory view
      const knownSkus = Array.from(window.TrackedSkus);
      if (knownSkus.length === 0 && items.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="3" class="state-unavailable">No inventory items tracked yet. Add a SKU above.</td></tr>`;
        if (paginationEl) paginationEl.innerHTML = '';
        return;
      }

      // Build unified list of SKU -> stock
      const inventoryMap = new Map();
      items.forEach(i => inventoryMap.set(i.sku, i.stock));

      const { pageItems, currentPage, totalPages, totalItems } =
        window.Paginator.slice(knownSkus, this.currentPage, this.PAGE_SIZE);
      this.currentPage = currentPage;

      let rowsHtml = '';
      pageItems.forEach(sku => {
        const stockVal = inventoryMap.has(sku) ? inventoryMap.get(sku) : '—';
        const isAvailable = stockVal !== '—';

        rowsHtml += `
          <tr>
            <td class="code-font">${this.escapeHtml(sku)}</td>
            <td><strong>${isAvailable ? stockVal : '<span class="text-muted">—</span>'}</strong></td>
            <td>
              <div style="display: flex; gap: 0.5rem;">
                <button class="btn btn-secondary btn-sm" onclick="window.InventoryPanelModule.adjustStock('${this.escapeHtml(sku)}', -5)">-5</button>
                <button class="btn btn-secondary btn-sm" onclick="window.InventoryPanelModule.adjustStock('${this.escapeHtml(sku)}', 5)">+5</button>
              </div>
            </td>
          </tr>
        `;
      });

      tableBody.innerHTML = rowsHtml;

      if (paginationEl) {
        paginationEl.innerHTML = window.Paginator.renderControls({ currentPage, totalPages, totalItems });
        window.Paginator.attach(paginationEl, this);
      }
    } catch (err) {
      console.warn('[InventoryPanel] Error fetching inventory:', err);
      tableBody.innerHTML = `<tr><td colspan="3" class="state-unavailable">Inventory data unavailable (${err.status || 'Error'}).</td></tr>`;
      if (paginationEl) paginationEl.innerHTML = '';
    }
  },

  handleAddTrackSku() {
    const input = document.getElementById('input-track-sku');
    const sku = input?.value?.trim();
    if (sku) {
      window.TrackedSkus.add(sku);
      input.value = '';
      if (window.refreshAllPanels) window.refreshAllPanels();
    }
  },

  async adjustStock(sku, delta) {
    try {
      await window.API.adjustInventory(sku, delta);
      if (window.refreshAllPanels) window.refreshAllPanels();
    } catch (err) {
      alert(`Failed to adjust stock for ${sku}: ${err.message || 'Error'}`);
    }
  },

  escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
};
