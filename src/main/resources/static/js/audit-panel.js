/**
 * audit-panel.js — renders the Audit Log panel from the real AuditController,
 * which returns raw formatted log lines (strings), not JSON objects, e.g.:
 *
 *   "[2026-08-09T22:10:21.588049Z] EXECUTE sku=SKU-1001 source=MANUAL originalPrice=24.99 newPrice=21.38"
 *
 * source is whatever triggered the reprice this entry came from -- "MANUAL" (a direct
 * Reprice-now/Undo click), "AUTO" (Observer-triggered by a stock change), or "SCHEDULED"
 * (the periodic sweep) -- rendered as its own Trigger column.
 *
 * This file parses that line format with a regex before rendering. If the
 * line doesn't match the expected shape, it's still shown (raw) rather than
 * silently dropped, so nothing gets hidden from the log.
 *
 * The SKU filter dropdown (populated from InventoryPanel's tracked-SKU list)
 * switches which endpoint render() calls -- GET /api/audit for "All SKUs",
 * GET /api/audit/{sku} for a specific one -- but reuses the same
 * parseLine/rowHtml pipeline either way.
 */

const AuditPanel = (() => {

  // sku is captured non-greedily up to the " source=" anchor (not \S+) since a SKU
  // entered with a space in it (e.g. "SKU 1003" instead of "SKU-1003") is still a
  // valid string as far as the backend is concerned -- nothing validates SKU format --
  // and would otherwise break this match, falling back to displaying the whole raw
  // log line unparsed.
  const LINE_PATTERN = /^\[(.+?)\]\s+(\S+)\s+sku=(.+?)\s+source=(\S+)\s+originalPrice=([\d.]+)\s+newPrice=([\d.]+)/;

  function parseLine(line) {
    const match = LINE_PATTERN.exec(line);
    if (!match) {
      return { raw: line };
    }
    const [, timestamp, action, sku, source, originalPrice, newPrice] = match;
    return { timestamp, action, sku, source, originalPrice: Number(originalPrice), newPrice: Number(newPrice) };
  }

  function rowHtml(entry) {
    if (entry.raw) {
      return `<tr><td colspan="6">${entry.raw}</td></tr>`;
    }
    const timestamp = new Date(entry.timestamp).toLocaleString();
    return `
      <tr>
        <td>${timestamp}</td>
        <td>${entry.action}</td>
        <td>${entry.sku}</td>
        <td>${entry.source}</td>
        <td>$${entry.originalPrice.toFixed(2)}</td>
        <td>$${entry.newPrice.toFixed(2)}</td>
      </tr>`;
  }

  /** Rebuilds the SKU filter's options from InventoryPanel's tracked-SKU list, preserving the current selection if it's still valid. */
  function populateSkuFilter() {
    const select = document.getElementById("audit-sku-filter");
    const previousValue = select.value;
    const trackedSkus = window.InventoryPanel ? window.InventoryPanel.getTrackedSkus() : [];

    const options = [`<option value="">All SKUs</option>`]
      .concat(trackedSkus.map((sku) => `<option value="${sku}">${sku}</option>`));
    select.innerHTML = options.join("");

    if (trackedSkus.includes(previousValue)) {
      select.value = previousValue;
    }
  }

  async function render() {
    const tbody = document.getElementById("audit-rows");
    populateSkuFilter();
    const selectedSku = document.getElementById("audit-sku-filter").value;

    try {
      const lines = selectedSku
        ? await Api.getAuditReportForSku(selectedSku) // string[], filtered to one SKU
        : await Api.getAuditReport(); // string[], full log

      if (!lines || lines.length === 0) {
        tbody.innerHTML = `<tr class="empty-row"><td colspan="6">No audit entries yet.</td></tr>`;
        return;
      }

      const parsed = lines.map(parseLine);
      // newest first — timestamps are ISO strings so string comparison sorts correctly
      const sorted = [...parsed].sort((a, b) => {
        const ta = a.timestamp || "";
        const tb = b.timestamp || "";
        return tb.localeCompare(ta);
      });

      tbody.innerHTML = sorted.map(rowHtml).join("");
    } catch (err) {
      console.error("Failed to load audit report", err);
      tbody.innerHTML = `<tr class="empty-row"><td colspan="6">Couldn't load audit log.</td></tr>`;
      Dashboard.showError(`Couldn't load audit log: ${err.message}`);
    }
  }

  function wireToolbar() {
    document.getElementById("audit-refresh-btn").addEventListener("click", render);
    document.getElementById("audit-sku-filter").addEventListener("change", render);
  }

  return { render, wireToolbar, parseLine };
})();

window.AuditPanel = AuditPanel;
