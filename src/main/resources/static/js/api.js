/**
 * api.js — the ONLY file in this dashboard that knows the backend's actual
 * endpoint paths and payload shapes. Every other panel file calls a function
 * here instead of building a URL itself, so if a controller's route or
 * response shape changes, this is the single place to update.
 *
 * Endpoint contract — verified directly against the real controller source
 * (InventoryController.java, PricingController.java, AuditController.java):
 *
 *   GET  /api/inventory                 -> { [sku]: quantity }  (full snapshot)
 *   GET  /api/inventory/{sku}           -> { sku, quantity }
 *   PUT  /api/inventory/{sku}?quantity=N          -> { sku, quantity }   (absolute set)
 *   POST /api/inventory/{sku}/adjust?delta=N      -> { sku, quantity }   (relative delta)
 *
 *   GET  /api/pricing/{sku}             -> { sku, prices: { [channel]: price } }
 *   POST /api/pricing/{sku}/reprice     -> { sku, prices: { [channel]: price } }
 *   GET  /api/pricing/{sku}/forecast    -> { sku, trend, strategy }  (read-only, no reprice triggered)
 *
 *   GET  /api/audit                     -> string[]  (raw AuditLoggingCommandDecorator lines,
 *                                           e.g. "[2026-08-09T22:10:21.588Z] EXECUTE sku=SKU-1001
 *                                           originalPrice=24.99 newPrice=21.38")
 *   GET  /api/audit/{sku}               -> string[]  (same format, filtered to one SKU)
 *
 */

const Api = (() => {
  const BASE = ""; // same-origin: dashboard is served by the same Spring Boot app

  async function request(path, options = {}) {
    const response = await fetch(BASE + path, options);

    if (!response.ok) {
      const text = await response.text().catch(() => "");
      throw new Error(`${options.method || "GET"} ${path} failed (${response.status}): ${text}`);
    }

    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      return response.json();
    }
    return response.text();
  }

  /** Full stock snapshot: { sku: quantity, ... } across every tracked SKU. */
  function getAllInventory() {
    return request(`/api/inventory`);
  }

  function getInventory(sku) {
    return request(`/api/inventory/${encodeURIComponent(sku)}`);
  }

  /** Relative stock change (positive = restock, negative = sale). delta is a query param, not a body. */
  function adjustStock(sku, delta) {
    const qs = new URLSearchParams({ delta }).toString();
    return request(`/api/inventory/${encodeURIComponent(sku)}/adjust?${qs}`, { method: "POST" });
  }

  /** Absolute stock set. quantity is a query param, not a body. */
  function setStock(sku, quantity) {
    const qs = new URLSearchParams({ quantity }).toString();
    return request(`/api/inventory/${encodeURIComponent(sku)}?${qs}`, { method: "PUT" });
  }

  function getPrices(sku) {
    return request(`/api/pricing/${encodeURIComponent(sku)}`);
  }

  function reprice(sku) {
    return request(`/api/pricing/${encodeURIComponent(sku)}/reprice`, { method: "POST" });
  }

  /** Read-only trend + selected-strategy preview: { sku, trend, strategy }. */
  function getForecast(sku) {
    return request(`/api/pricing/${encodeURIComponent(sku)}/forecast`);
  }

  /** Full audit trail as raw log-line strings. */
  function getAuditReport() {
    return request(`/api/audit`);
  }

  /** Audit trail filtered to a single SKU, as raw log-line strings. */
  function getAuditReportForSku(sku) {
    return request(`/api/audit/${encodeURIComponent(sku)}`);
  }

  return {
    getAllInventory, getInventory, adjustStock, setStock,
    getPrices, reprice, getForecast,
    getAuditReport, getAuditReportForSku,
  };
})();
