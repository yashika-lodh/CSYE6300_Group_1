/**
 * Panel 6 — Demand Forecast Panel (Read-only Strategy Decisions)
 */

window.ForecastPanelModule = {
  currentPage: 1,
  PAGE_SIZE: 5,

  async refresh() {
    const tableBody = document.getElementById('forecast-table-body');
    const paginationEl = document.getElementById('forecast-pagination');
    if (!tableBody) return;

    const trackedSkus = Array.from(window.TrackedSkus);
    if (trackedSkus.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="3" class="state-unavailable">No tracked SKUs available.</td></tr>`;
      if (paginationEl) paginationEl.innerHTML = '';
      return;
    }

    const { pageItems: pagedSkus, currentPage, totalPages, totalItems } =
      window.Paginator.slice(trackedSkus, this.currentPage, this.PAGE_SIZE);
    this.currentPage = currentPage;

    let rowsHtml = '';

    for (const sku of pagedSkus) {
      try {
        const forecast = await window.API.getForecast(sku);
        const trend = (forecast.trend || 'STABLE').toUpperCase();
        const strategy = forecast.strategy || forecast.pricingStrategy || 'StandardPricingStrategy';

        let badgeClass = 'badge-gray';
        if (trend === 'RISING' || trend === 'HIGH') badgeClass = 'badge-green';
        else if (trend === 'FALLING' || trend === 'LOW') badgeClass = 'badge-red';

        rowsHtml += `
          <tr>
            <td class="code-font">${this.escapeHtml(sku)}</td>
            <td>
              <span class="badge ${badgeClass}">${this.escapeHtml(trend)}</span>
            </td>
            <td>
              <span class="code-font">${this.escapeHtml(strategy)}</span>
            </td>
          </tr>
        `;
      } catch (err) {
        rowsHtml += `
          <tr>
            <td class="code-font">${this.escapeHtml(sku)}</td>
            <td colspan="2" class="state-unavailable">Forecast unavailable (${err.status || 'Error'})</td>
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

  escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
};
