/**
 * Panel 3 — Pricing Panel & Global Undo Command History
 */

window.PricingPanelModule = {
  currentPage: 1,
  PAGE_SIZE: 5,

  init() {
    const undoBtn = document.getElementById('btn-global-undo');
    if (undoBtn) {
      undoBtn.addEventListener('click', () => this.handleUndoLastCommand());
    }
  },

  async refresh() {
    const tableBody = document.getElementById('pricing-table-body');
    const paginationEl = document.getElementById('pricing-pagination');
    if (!tableBody) return;

    const trackedSkus = Array.from(window.TrackedSkus);
    if (trackedSkus.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="4" class="state-unavailable">No tracked SKUs available.</td></tr>`;
      if (paginationEl) paginationEl.innerHTML = '';
      return;
    }

    // Paginate by SKU (not by raw row) since each SKU spans multiple channel rows via rowspan.
    const { pageItems: pagedSkus, currentPage, totalPages, totalItems } =
      window.Paginator.slice(trackedSkus, this.currentPage, this.PAGE_SIZE);
    this.currentPage = currentPage;

    let allRowsHtml = '';

    // Fetch pricing for every SKU on this page
    for (const sku of pagedSkus) {
      try {
        const pricingData = await window.API.getPricing(sku);
        let channelList = [];

        if (Array.isArray(pricingData)) {
          channelList = pricingData;
        } else if (pricingData && Array.isArray(pricingData.channels)) {
          channelList = pricingData.channels.map(c => ({
            channel: c.channel || c.name,
            price: c.price
          }));
        } else if (pricingData && pricingData.prices && typeof pricingData.prices === 'object') {
          // Real shape from PricingController: { sku, prices: { "Shopify": 24.30, ... } }
          channelList = Object.entries(pricingData.prices).map(([channel, price]) => ({ channel, price }));
        } else if (pricingData && typeof pricingData === 'object') {
          // Could be {"Shopify Direct": 89.99, "Own Web Store": 89.99}
          channelList = Object.entries(pricingData).map(([channel, price]) => ({ channel, price }));
        }

        if (channelList.length === 0) {
          allRowsHtml += `
            <tr>
              <td class="code-font">${this.escapeHtml(sku)}</td>
              <td colspan="2" class="state-unavailable">No active channel prices</td>
              <td>
                <button class="btn btn-primary btn-sm" onclick="window.PricingPanelModule.repriceSku('${this.escapeHtml(sku)}')">Reprice Now</button>
              </td>
            </tr>
          `;
        } else {
          const rowCount = channelList.length;
          channelList.forEach((ch, idx) => {
            const formattedPrice = typeof ch.price === 'number' ? `$${ch.price.toFixed(2)}` : (ch.price || '—');
            allRowsHtml += `
              <tr>
                ${idx === 0 ? `<td class="code-font" rowspan="${rowCount}">${this.escapeHtml(sku)}</td>` : ''}
                <td>${this.escapeHtml(ch.channel || 'Direct Channel')}</td>
                <td><strong>${formattedPrice}</strong></td>
                ${idx === 0 ? `
                  <td rowspan="${rowCount}">
                    <button class="btn btn-primary btn-sm" onclick="window.PricingPanelModule.repriceSku('${this.escapeHtml(sku)}')">Reprice Now</button>
                  </td>
                ` : ''}
              </tr>
            `;
          });
        }
      } catch (err) {
        allRowsHtml += `
          <tr>
            <td class="code-font">${this.escapeHtml(sku)}</td>
            <td colspan="2" class="state-unavailable">Pricing data unavailable (${err.status || 'Error'})</td>
            <td>
              <button class="btn btn-primary btn-sm" onclick="window.PricingPanelModule.repriceSku('${this.escapeHtml(sku)}')">Reprice Now</button>
            </td>
          </tr>
        `;
      }
    }

    tableBody.innerHTML = allRowsHtml;

    if (paginationEl) {
      paginationEl.innerHTML = window.Paginator.renderControls({ currentPage, totalPages, totalItems });
      window.Paginator.attach(paginationEl, this);
    }
  },

  async repriceSku(sku) {
    const statusMsg = document.getElementById('pricing-status-msg');
    try {
      if (statusMsg) {
        statusMsg.className = 'status-msg info';
        statusMsg.style.display = 'flex';
        statusMsg.innerHTML = `<span>Triggering RepricingWorkflow for ${sku}...</span>`;
      }
      const result = await window.API.repriceSku(sku);
      if (statusMsg) {
        statusMsg.className = 'status-msg success';
        statusMsg.innerHTML = `<span>Repriced ${sku} successfully!</span>`;
      }
      if (window.refreshAllPanels) window.refreshAllPanels();
    } catch (err) {
      if (statusMsg) {
        statusMsg.className = 'status-msg error';
        statusMsg.innerHTML = `<span>Failed to reprice ${sku}: ${err.message || 'Error'}</span>`;
      }
    }
  },

  async handleUndoLastCommand() {
    const undoBtn = document.getElementById('btn-global-undo');
    const statusMsg = document.getElementById('pricing-status-msg');

    try {
      const res = await window.API.undoLastCommand();
      if (res && res.undone) {
        const skuInfo = res.sku ? `for ${res.sku}` : '';
        const detail = `Undone last pricing command ${skuInfo}: $${res.originalPrice || ''} → $${res.newPrice || ''}`;
        if (statusMsg) {
          statusMsg.className = 'status-msg success';
          statusMsg.style.display = 'flex';
          statusMsg.innerHTML = `<span>${detail}</span>`;
        }
      } else {
        if (statusMsg) {
          statusMsg.className = 'status-msg info';
          statusMsg.style.display = 'flex';
          statusMsg.innerHTML = `<span>No pricing commands available in undo history stack.</span>`;
        }
      }
      if (window.refreshAllPanels) window.refreshAllPanels();
    } catch (err) {
      if (err && err.status === 404) {
        if (undoBtn) {
          undoBtn.disabled = true;
          undoBtn.setAttribute('data-tooltip', 'Not available yet.');
        }
        if (statusMsg) {
          statusMsg.className = 'status-msg error';
          statusMsg.style.display = 'flex';
          statusMsg.innerHTML = `<span>Undo Endpoint Not Available (404).</span>`;
        }
      } else {
        if (statusMsg) {
          statusMsg.className = 'status-msg error';
          statusMsg.style.display = 'flex';
          statusMsg.innerHTML = `<span>Undo Failed: ${err.message || 'Error'}</span>`;
        }
      }
    }
  },

  escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
};
