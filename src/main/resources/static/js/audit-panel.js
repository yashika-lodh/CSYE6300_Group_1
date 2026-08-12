/**
 * Panel 7 — Audit Log Panel (Compliance Trail & Filter)
 */

window.AuditPanelModule = {
  currentPage: 1,
  PAGE_SIZE: 8,

  init() {
    const filterSelect = document.getElementById('audit-sku-filter');
    if (filterSelect) {
      filterSelect.addEventListener('change', () => {
        this.currentPage = 1;
        this.refresh();
      });
    }

    const refreshBtn = document.getElementById('btn-refresh-audit');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', () => this.refresh());
    }
  },

  async refresh() {
    this.updateSkuDropdown();

    const tableBody = document.getElementById('audit-table-body');
    const paginationEl = document.getElementById('audit-pagination');
    const filterSelect = document.getElementById('audit-sku-filter');
    if (!tableBody) return;

    const selectedSku = filterSelect?.value || 'ALL';

    try {
      const logsData = await window.API.getAuditLogs(selectedSku);

      let logEntries = [];
      if (Array.isArray(logsData)) {
        logEntries = logsData;
      } else if (logsData && Array.isArray(logsData.logs)) {
        logEntries = logsData.logs;
      }

      if (logEntries.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="6" class="state-unavailable">No audit log entries recorded yet.</td></tr>`;
        if (paginationEl) paginationEl.innerHTML = '';
        return;
      }

      // Most recent entries first, then paginate.
      const orderedEntries = [...logEntries].reverse();
      const { pageItems: pagedEntries, currentPage, totalPages, totalItems } =
        window.Paginator.slice(orderedEntries, this.currentPage, this.PAGE_SIZE);
      this.currentPage = currentPage;

      let rowsHtml = '';

      pagedEntries.forEach(entry => {
        const parsed = this.parseLogEntry(entry);
        const triggerBadge = this.getTriggerBadge(parsed.trigger);

        rowsHtml += `
          <tr>
            <td class="code-font">${this.escapeHtml(parsed.timestamp)}</td>
            <td><span class="badge badge-blue">${this.escapeHtml(parsed.action)}</span></td>
            <td class="code-font">${this.escapeHtml(parsed.sku)}</td>
            <td>${parsed.originalPrice ? `$${parsed.originalPrice}` : '—'}</td>
            <td><strong>${parsed.newPrice ? `$${parsed.newPrice}` : '—'}</strong></td>
            <td>${triggerBadge}</td>
          </tr>
        `;
      });

      tableBody.innerHTML = rowsHtml;

      if (paginationEl) {
        paginationEl.innerHTML = window.Paginator.renderControls({ currentPage, totalPages, totalItems });
        window.Paginator.attach(paginationEl, this);
      }
    } catch (err) {
      tableBody.innerHTML = `<tr><td colspan="6" class="state-unavailable">Audit log unavailable (${err.status || 'Error'}).</td></tr>`;
      if (paginationEl) paginationEl.innerHTML = '';
    }
  },

  updateSkuDropdown() {
    const filterSelect = document.getElementById('audit-sku-filter');
    if (!filterSelect) return;

    const currentValue = filterSelect.value;
    const trackedSkus = Array.from(window.TrackedSkus);

    let optionsHtml = `<option value="ALL">All SKUs</option>`;
    trackedSkus.forEach(sku => {
      const selected = sku === currentValue ? 'selected' : '';
      optionsHtml += `<option value="${this.escapeHtml(sku)}" ${selected}>${this.escapeHtml(sku)}</option>`;
    });

    filterSelect.innerHTML = optionsHtml;
  },

  parseLogEntry(entry) {
    if (typeof entry === 'object' && entry !== null) {
      return {
        timestamp: entry.timestamp || entry.time || new Date().toISOString(),
        action: entry.action || 'EXECUTE',
        sku: entry.sku || '—',
        originalPrice: entry.originalPrice !== undefined ? entry.originalPrice : null,
        newPrice: entry.newPrice !== undefined ? entry.newPrice : null,
        trigger: entry.source || entry.trigger || '—'
      };
    }

    const str = String(entry);
    
    // Pattern: "[2026-08-10T20:00:00Z] EXECUTE sku=SKU-101 originalPrice=100.00 newPrice=89.99 source=MANUAL"
    const timestampMatch = str.match(/\[(.*?)\]/);
    const actionMatch = str.match(/\]\s+([A-Z]+)/);
    const skuMatch = str.match(/sku=([^\s]+)/);
    const origMatch = str.match(/originalPrice=([\d.]+)/);
    const newMatch = str.match(/newPrice=([\d.]+)/);
    const sourceMatch = str.match(/source=([^\s]+)/);

    return {
      timestamp: timestampMatch ? timestampMatch[1] : '—',
      action: actionMatch ? actionMatch[1] : 'EXECUTE',
      sku: skuMatch ? skuMatch[1] : '—',
      originalPrice: origMatch ? origMatch[1] : null,
      newPrice: newMatch ? newMatch[1] : null,
      trigger: sourceMatch ? sourceMatch[1] : '—'
    };
  },

  getTriggerBadge(trigger) {
    if (!trigger || trigger === '—') {
      return `<span class="text-muted">—</span>`;
    }
    const upper = trigger.toUpperCase();
    if (upper === 'MANUAL') return `<span class="badge badge-blue">MANUAL</span>`;
    // RepricingTriggerObserver tags Observer-triggered reprices "AUTO" (not "OBSERVER").
    if (upper === 'AUTO' || upper === 'OBSERVER') return `<span class="badge badge-green">${this.escapeHtml(upper)}</span>`;
    if (upper === 'SCHEDULED') return `<span class="badge badge-gray">SCHEDULED</span>`;
    return `<span class="badge badge-gray">${this.escapeHtml(upper)}</span>`;
  },

  escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
};
