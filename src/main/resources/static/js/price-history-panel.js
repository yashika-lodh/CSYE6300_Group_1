/**
 * price-history-panel.js — renders the Price History panel: one step-line
 * per tracked SKU showing price over time, built entirely from data already
 * being logged -- AuditLoggingCommandDecorator's compliance trail (the same
 * GET /api/audit the Audit Log panel reads), reusing AuditPanel.parseLine so
 * the log-line format is parsed in exactly one place. No new backend
 * endpoint. A price is a step function (it only changes at a logged
 * EXECUTE/UNDO event and holds until the next one), so the line is drawn as
 * a step-after path, not a smoothed/interpolated one.
 *
 * Chart follows the project's dataviz conventions: categorical color
 * assigned in a fixed order per SKU (stable across renders, keyed by SKU so
 * color follows identity, not position in the tracked-SKU list), a legend
 * for 2+ series, direct end-labels capped at 4 series, a crosshair+tooltip
 * reading every series at once, and a plain-HTML table view as a fallback
 * (also the required "relief" for the three categorical hues that sit below
 * 3:1 contrast on a white surface -- see css/chart.css).
 */

const PriceHistoryPanel = (() => {

  const SERIES_COLOR_VARS = [
    "--series-1", "--series-2", "--series-3", "--series-4",
    "--series-5", "--series-6", "--series-7", "--series-8",
  ];
  const MAX_SERIES = 8;
  const DIRECT_LABEL_LIMIT = 4;

  // Assigns each SKU a color slot the first time it's seen and keeps it for
  // the life of the page, so a SKU's line color never changes across
  // refreshes even if other SKUs are added/removed from the tracked list.
  const skuColorAssignments = new Map();

  function colorVarFor(sku) {
    if (!skuColorAssignments.has(sku)) {
      skuColorAssignments.set(sku, SERIES_COLOR_VARS[skuColorAssignments.size % SERIES_COLOR_VARS.length]);
    }
    return skuColorAssignments.get(sku);
  }

  async function loadSeriesBySku(trackedSkus) {
    let lines;
    try {
      lines = await Api.getAuditReport();
    } catch (err) {
      console.error("Failed to load audit report for price history", err);
      throw err;
    }

    const trackedSet = new Set(trackedSkus);
    const bySku = new Map();

    for (const line of lines) {
      const entry = AuditPanel.parseLine(line);
      if (entry.raw || !trackedSet.has(entry.sku)) continue;
      const t = Date.parse(entry.timestamp);
      if (Number.isNaN(t)) continue;
      if (!bySku.has(entry.sku)) bySku.set(entry.sku, []);
      bySku.get(entry.sku).push({ t, price: entry.newPrice });
    }

    for (const points of bySku.values()) {
      points.sort((a, b) => a.t - b.t);
    }

    return bySku;
  }

  function niceTicks(min, max, count) {
    if (min === max) {
      return [min];
    }
    const step = (max - min) / (count - 1);
    return Array.from({ length: count }, (_, i) => min + step * i);
  }

  function formatTime(t) {
    return new Date(t).toLocaleString(undefined, {
      month: "short", day: "numeric", hour: "numeric", minute: "2-digit",
    });
  }

  function buildStepPath(points, xScale, yScale, xMax) {
    if (points.length === 0) return "";
    let d = `M ${xScale(points[0].t)} ${yScale(points[0].price)}`;
    for (let i = 1; i < points.length; i++) {
      d += ` L ${xScale(points[i].t)} ${yScale(points[i - 1].price)}`;
      d += ` L ${xScale(points[i].t)} ${yScale(points[i].price)}`;
    }
    // Hold the last known price flat through to the right edge -- it's
    // still the current price even though nothing has re-logged it since.
    d += ` L ${xScale(xMax)} ${yScale(points[points.length - 1].price)}`;
    return d;
  }

  /** Value each series holds at time t (its most recent point at or before t). */
  function valueAt(points, t) {
    let current = points[0];
    for (const p of points) {
      if (p.t > t) break;
      current = p;
    }
    return current;
  }

  function renderChart(container, bySku) {
    const skus = [...bySku.keys()].slice(0, MAX_SERIES);
    const overflowCount = bySku.size - skus.length;

    const allPoints = skus.flatMap((sku) => bySku.get(sku));
    const now = Date.now();
    const tMin = Math.min(...allPoints.map((p) => p.t));
    const tMax = Math.max(now, ...allPoints.map((p) => p.t));
    const priceMin = Math.min(...allPoints.map((p) => p.price));
    const priceMax = Math.max(...allPoints.map((p) => p.price));
    const pad = Math.max((priceMax - priceMin) * 0.15, 1);
    const yMin = Math.max(0, priceMin - pad);
    const yMax = priceMax + pad;

    const width = 860;
    const height = 180;
    const marginLeft = 56;
    const marginRight = 16;
    const marginTop = 12;
    const marginBottom = 24;
    const plotWidth = width - marginLeft - marginRight;
    const plotHeight = height - marginTop - marginBottom;

    const xScale = (t) => marginLeft + (tMax === tMin ? plotWidth / 2 : ((t - tMin) / (tMax - tMin)) * plotWidth);
    const yScale = (v) => marginTop + plotHeight - ((v - yMin) / (yMax - yMin || 1)) * plotHeight;

    const yTicks = niceTicks(yMin, yMax, 4);
    const xTicks = niceTicks(tMin, tMax, 4);

    const gridlines = yTicks.map((v) =>
      `<line class="gridline" x1="${marginLeft}" y1="${yScale(v)}" x2="${width - marginRight}" y2="${yScale(v)}" />`
    ).join("");

    const yLabels = yTicks.map((v) =>
      `<text class="axis-label" x="${marginLeft - 8}" y="${yScale(v) + 4}" text-anchor="end">$${v.toFixed(2)}</text>`
    ).join("");

    const xLabels = xTicks.map((t) =>
      `<text class="axis-label" x="${xScale(t)}" y="${height - 6}" text-anchor="middle">${formatTime(t)}</text>`
    ).join("");

    const axisLine = `<line class="axis-line" x1="${marginLeft}" y1="${marginTop + plotHeight}" x2="${width - marginRight}" y2="${marginTop + plotHeight}" />`;

    const showDirectLabels = skus.length <= DIRECT_LABEL_LIMIT;

    const seriesMarkup = skus.map((sku) => {
      const points = bySku.get(sku);
      const colorVar = `var(${colorVarFor(sku)})`;
      const path = buildStepPath(points, xScale, yScale, tMax);
      const lastPoint = points[points.length - 1];
      const endX = xScale(tMax);
      const endY = yScale(lastPoint.price);

      const marker = `<circle class="series-marker" cx="${endX}" cy="${endY}" r="5" fill="${colorVar}" />`;
      const label = showDirectLabels
        ? `<text class="direct-label" x="${endX + 8}" y="${endY + 4}">${escapeXml(sku)} $${lastPoint.price.toFixed(2)}</text>`
        : "";

      return `<path class="series-line" d="${path}" stroke="${colorVar}" data-sku="${escapeXml(sku)}" />${marker}${label}`;
    }).join("");

    const legend = skus.length >= 2
      ? `<div class="chart-legend">${skus.map((sku) =>
          `<span class="chart-legend-item"><span class="chart-legend-swatch" style="background: var(${colorVarFor(sku)})"></span>${escapeHtml(sku)}</span>`
        ).join("")}</div>`
      : "";

    const overflowNote = overflowCount > 0
      ? `<p class="panel-subtitle">+${overflowCount} more tracked SKU(s) not shown — chart caps at ${MAX_SERIES} series.</p>`
      : "";

    container.innerHTML = `
      <div class="viz-root" style="position: relative;">
        <svg class="chart-svg" viewBox="0 0 ${width} ${height}" data-width="${width}" data-margin-left="${marginLeft}" data-t-min="${tMin}" data-t-max="${tMax}">
          ${gridlines}
          ${axisLine}
          ${yLabels}
          ${xLabels}
          ${seriesMarkup}
          <line class="crosshair-line" x1="0" y1="${marginTop}" x2="0" y2="${marginTop + plotHeight}" style="display: none;" />
        </svg>
        ${legend}
        ${overflowNote}
      </div>
    `;

    wireHover(container, bySku, skus, { xScale, tMin, tMax, marginTop, plotHeight, marginLeft, plotWidth, width, height });
  }

  function wireHover(container, bySku, skus, geometry) {
    const svg = container.querySelector("svg");
    const crosshair = container.querySelector(".crosshair-line");
    let tooltip = container.querySelector(".chart-tooltip");
    if (!tooltip) {
      tooltip = document.createElement("div");
      tooltip.className = "chart-tooltip";
      tooltip.style.display = "none";
      container.querySelector(".viz-root").appendChild(tooltip);
    }

    function onMove(event) {
      const rect = svg.getBoundingClientRect();
      const scaleX = geometry.width / rect.width;
      const svgX = (event.clientX - rect.left) * scaleX;
      const clampedX = Math.min(Math.max(svgX, geometry.marginLeft), geometry.marginLeft + geometry.plotWidth);
      const t = geometry.tMin + ((clampedX - geometry.marginLeft) / geometry.plotWidth) * (geometry.tMax - geometry.tMin);

      crosshair.setAttribute("x1", clampedX);
      crosshair.setAttribute("x2", clampedX);
      crosshair.style.display = "block";

      const rows = skus.map((sku) => {
        const v = valueAt(bySku.get(sku), t);
        return `<div class="chart-tooltip-row"><span class="chart-tooltip-key" style="background: var(${colorVarFor(sku)})"></span><span class="chart-tooltip-sku">${escapeHtml(sku)}</span><span class="chart-tooltip-value">$${v.price.toFixed(2)}</span></div>`;
      }).join("");
      tooltip.innerHTML = `<div class="chart-tooltip-row" style="color:#d5d5d5; margin-bottom:4px;">${formatTime(t)}</div>${rows}`;

      const containerRect = container.getBoundingClientRect();
      tooltip.style.left = `${event.clientX - containerRect.left}px`;
      tooltip.style.top = `${event.clientY - containerRect.top - 12}px`;
      tooltip.style.display = "block";
    }

    function onLeave() {
      crosshair.style.display = "none";
      tooltip.style.display = "none";
    }

    svg.addEventListener("pointermove", onMove);
    svg.addEventListener("pointerleave", onLeave);
  }

  function renderTable(container, bySku) {
    const rows = [];
    for (const [sku, points] of bySku) {
      for (const p of points) {
        rows.push({ sku, t: p.t, price: p.price });
      }
    }
    rows.sort((a, b) => b.t - a.t);

    if (rows.length === 0) {
      container.innerHTML = `<p class="chart-empty">No price history yet.</p>`;
      return;
    }

    const body = rows.map((r) =>
      `<tr><td>${formatTime(r.t)}</td><td>${escapeHtml(r.sku)}</td><td>$${r.price.toFixed(2)}</td></tr>`
    ).join("");

    container.innerHTML = `
      <table class="data-table">
        <thead><tr><th>Timestamp</th><th>SKU</th><th>Price</th></tr></thead>
        <tbody>${body}</tbody>
      </table>
    `;
  }

  function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
  }

  // SVG text content follows the same untrusted-data rule as HTML -- escape
  // before interpolating into markup, since a SKU name can be arbitrary user input.
  function escapeXml(str) {
    return String(str).replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&apos;",
    }[c]));
  }

  let showingTable = false;

  async function render() {
    const container = document.getElementById("price-history-body");
    const skus = window.InventoryPanel ? window.InventoryPanel.getTrackedSkus() : [];

    if (skus.length === 0) {
      container.innerHTML = `<p class="chart-empty">No SKUs tracked yet — add one in the Inventory panel.</p>`;
      return;
    }

    let bySku;
    try {
      bySku = await loadSeriesBySku(skus);
    } catch (err) {
      container.innerHTML = `<p class="chart-empty">Couldn't load price history.</p>`;
      Dashboard.showError(`Couldn't load price history: ${err.message}`);
      return;
    }

    if (bySku.size === 0) {
      container.innerHTML = `<p class="chart-empty">No price changes logged yet for the tracked SKUs — trigger a reprice to see history.</p>`;
      return;
    }

    if (showingTable) {
      renderTable(container, bySku);
    } else {
      renderChart(container, bySku);
    }
  }

  function wireToolbar() {
    document.getElementById("price-history-toggle-btn").addEventListener("click", async (event) => {
      showingTable = !showingTable;
      event.currentTarget.textContent = showingTable ? "View as chart" : "View as table";
      await render();
    });
  }

  return { render, wireToolbar };
})();

window.PriceHistoryPanel = PriceHistoryPanel;
