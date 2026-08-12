/**
 * Panel 5 — Price History Chart & Accessible Table View
 */

window.PriceHistoryModule = {
  isTableView: false,
  currentPage: 1,
  PAGE_SIZE: 8,

  init() {
    const toggleBtn = document.getElementById('btn-toggle-price-history-view');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', () => {
        this.isTableView = !this.isTableView;
        toggleBtn.textContent = this.isTableView ? 'View as Chart' : 'View as Table';
        this.currentPage = 1;
        this.render();
      });
    }
  },

  async refresh() {
    this.render();
  },

  async render() {
    const chartContainer = document.getElementById('price-history-chart-container');
    const tableContainer = document.getElementById('price-history-table-container');
    const paginationEl = document.getElementById('price-history-pagination');

    if (!chartContainer || !tableContainer) return;

    try {
      const logs = await window.API.getAuditLogs();
      const parsedPoints = this.parseAuditLogsForHistory(logs);

      if (this.isTableView) {
        chartContainer.style.display = 'none';
        tableContainer.style.display = 'block';
        this.renderTable(parsedPoints, tableContainer, paginationEl);
      } else {
        tableContainer.style.display = 'none';
        chartContainer.style.display = 'block';
        if (paginationEl) paginationEl.innerHTML = '';
        this.renderChart(parsedPoints, chartContainer);
      }
    } catch (err) {
      chartContainer.innerHTML = `<div class="state-unavailable">Price history data unavailable (${err.status || 'Error'}).</div>`;
      tableContainer.innerHTML = `<div class="state-unavailable">Price history table unavailable.</div>`;
      if (paginationEl) paginationEl.innerHTML = '';
    }
  },

  parseAuditLogsForHistory(logs) {
    // Array of log strings or log objects
    let entries = [];
    if (Array.isArray(logs)) {
      entries = logs;
    } else if (logs && Array.isArray(logs.logs)) {
      entries = logs.logs;
    }

    const pointsBySku = new Map();

    entries.forEach(entry => {
      let time, sku, newPrice, oldPrice;

      if (typeof entry === 'string') {
        // e.g. "[2026-08-10T20:00:00Z] EXECUTE sku=SKU-101 originalPrice=100.00 newPrice=89.99 source=MANUAL"
        const timeMatch = entry.match(/\[(.*?)\]/);
        const skuMatch = entry.match(/sku=([^\s]+)/);
        const priceMatch = entry.match(/newPrice=([\d.]+)/);
        const origMatch = entry.match(/originalPrice=([\d.]+)/);

        if (timeMatch) time = timeMatch[1];
        if (skuMatch) sku = skuMatch[1];
        if (priceMatch) newPrice = parseFloat(priceMatch[1]);
        if (origMatch) oldPrice = parseFloat(origMatch[1]);
      } else if (entry && typeof entry === 'object') {
        time = entry.timestamp || entry.time;
        sku = entry.sku;
        newPrice = parseFloat(entry.newPrice);
        oldPrice = parseFloat(entry.originalPrice);
      }

      if (sku && !isNaN(newPrice)) {
        if (!pointsBySku.has(sku)) pointsBySku.set(sku, []);
        const list = pointsBySku.get(sku);
        
        // If first point, add initial price point if available
        if (list.length === 0 && !isNaN(oldPrice)) {
          const initTime = time ? new Date(new Date(time).getTime() - 60000).toISOString() : new Date().toISOString();
          list.push({ time: initTime, price: oldPrice, sku });
        }
        
        list.push({ time: time || new Date().toISOString(), price: newPrice, sku });
      }
    });

    return pointsBySku;
  },

  renderChart(pointsBySku, container) {
    if (!pointsBySku || pointsBySku.size === 0) {
      container.innerHTML = `<div class="state-unavailable">No price change points recorded in history yet.</div>`;
      return;
    }

    // Colors per SKU
    const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4'];
    let colorIdx = 0;

    let minPrice = Infinity;
    let maxPrice = -Infinity;
    let allTimes = [];

    pointsBySku.forEach((points) => {
      points.forEach(pt => {
        if (pt.price < minPrice) minPrice = pt.price;
        if (pt.price > maxPrice) maxPrice = pt.price;
        const t = new Date(pt.time).getTime();
        if (!isNaN(t)) allTimes.push(t);
      });
    });

    if (minPrice === Infinity) { minPrice = 0; maxPrice = 100; }
    if (minPrice === maxPrice) { minPrice -= 10; maxPrice += 10; }
    
    // Add padding to Y axis
    const yMargin = (maxPrice - minPrice) * 0.15 || 5;
    minPrice = Math.max(0, minPrice - yMargin);
    maxPrice = maxPrice + yMargin;

    let minTime = Math.min(...allTimes);
    let maxTime = Math.max(...allTimes);
    if (minTime === maxTime) {
      minTime -= 300000;
      maxTime += 300000;
    }

    const width = 800;
    const height = 300;
    const padding = { top: 30, right: 120, bottom: 40, left: 60 };
    const chartW = width - padding.left - padding.right;
    const chartH = height - padding.top - padding.bottom;

    const scaleX = (time) => padding.left + ((time - minTime) / (maxTime - minTime)) * chartW;
    const scaleY = (price) => padding.top + chartH - ((price - minPrice) / (maxPrice - minPrice)) * chartH;

    let svgPaths = '';
    let legendHtml = '';

    pointsBySku.forEach((points, sku) => {
      const color = colors[colorIdx % colors.length];
      colorIdx++;

      // Sort points by time
      points.sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime());

      // Build step line SVG path (horizontal line then vertical step)
      let d = '';
      let circles = '';

      points.forEach((pt, i) => {
        const x = scaleX(new Date(pt.time).getTime());
        const y = scaleY(pt.price);

        if (i === 0) {
          d += `M ${x} ${y}`;
        } else {
          // Step change: maintain previous Y until current X, then step to current Y
          const prevY = scaleY(points[i - 1].price);
          d += ` L ${x} ${prevY} L ${x} ${y}`;
        }

        circles += `<circle cx="${x}" cy="${y}" r="4" fill="${color}" stroke="#0f172a" stroke-width="2" />`;
      });

      svgPaths += `<path d="${d}" fill="none" stroke="${color}" stroke-width="2.5" stroke-linejoin="round" />`;
      svgPaths += circles;

      legendHtml += `
        <div style="display: flex; align-items: center; gap: 0.4rem; font-size: 0.8rem;">
          <span style="width: 10px; height: 10px; background-color: ${color}; border-radius: 2px;"></span>
          <span class="code-font" style="color: #f8fafc;">${this.escapeHtml(sku)}</span>
        </div>
      `;
    });

    // Draw Y axis lines
    let yAxisHtml = '';
    const yTicks = 4;
    for (let i = 0; i <= yTicks; i++) {
      const p = minPrice + (i / yTicks) * (maxPrice - minPrice);
      const y = scaleY(p);
      yAxisHtml += `
        <line x1="${padding.left}" y1="${y}" x2="${width - padding.right}" y2="${y}" stroke="#334155" stroke-dasharray="3,3" />
        <text x="${padding.left - 8}" y="${y + 4}" fill="#94a3b8" font-size="11" text-anchor="end">$${p.toFixed(2)}</text>
      `;
    }

    container.innerHTML = `
      <div style="display: flex; flex-direction: column; gap: 0.75rem;">
        <div style="display: flex; gap: 1rem; flex-wrap: wrap;">${legendHtml}</div>
        <svg viewBox="0 0 ${width} ${height}" style="width: 100%; height: auto; background-color: #0f172a; border-radius: 8px;">
          ${yAxisHtml}
          ${svgPaths}
        </svg>
      </div>
    `;
  },

  renderTable(pointsBySku, container, paginationEl) {
    // Flatten all SKUs' points into one chronological list (most recent first) to paginate.
    let allPoints = [];
    pointsBySku.forEach((points, sku) => {
      points.forEach(pt => allPoints.push({ sku, price: pt.price, time: pt.time }));
    });
    allPoints.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime());

    if (allPoints.length === 0) {
      container.innerHTML = `<div class="state-unavailable">No historical price entries found.</div>`;
      if (paginationEl) paginationEl.innerHTML = '';
      return;
    }

    const { pageItems, currentPage, totalPages, totalItems } =
      window.Paginator.slice(allPoints, this.currentPage, this.PAGE_SIZE);
    this.currentPage = currentPage;

    const rows = pageItems.map(pt => {
      const timeStr = pt.time ? new Date(pt.time).toLocaleString() : '—';
      return `
        <tr>
          <td class="code-font">${this.escapeHtml(pt.sku)}</td>
          <td>$${pt.price.toFixed(2)}</td>
          <td class="code-font">${this.escapeHtml(timeStr)}</td>
        </tr>
      `;
    }).join('');

    container.innerHTML = `
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>New Price</th>
              <th>Timestamp</th>
            </tr>
          </thead>
          <tbody>
            ${rows}
          </tbody>
        </table>
      </div>
    `;

    if (paginationEl) {
      paginationEl.innerHTML = window.Paginator.renderControls({ currentPage, totalPages, totalItems });
      window.Paginator.attach(paginationEl, this);
    }
  },

  escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
};
