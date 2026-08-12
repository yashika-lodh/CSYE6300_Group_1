/**
 * Main Application Orchestrator for Live Dashboard
 */

// Global tracked SKUs set across all panels. Starts empty -- InventoryPanel's
// refresh() merges in every real SKU from GET /api/inventory on first load, so
// there's no need (and no accurate way) to guess placeholder SKUs up front.
window.TrackedSkus = new Set();

// Global cached pricing inputs (minPrice, maxPrice, cost, etc.)
window.SkuInputsMap = new Map();

/**
 * Refreshes all dashboard panels in parallel and updates footer timestamp
 */
window.refreshAllPanels = async function() {
  const refreshPromises = [];

  if (window.SystemStatusModule?.refresh) {
    refreshPromises.push(window.SystemStatusModule.refresh().catch(e => console.warn(e)));
  }
  if (window.InventoryPanelModule?.refresh) {
    refreshPromises.push(window.InventoryPanelModule.refresh().catch(e => console.warn(e)));
  }
  if (window.PricingPanelModule?.refresh) {
    refreshPromises.push(window.PricingPanelModule.refresh().catch(e => console.warn(e)));
  }
  if (window.GuardrailsPanelModule?.refresh) {
    refreshPromises.push(window.GuardrailsPanelModule.refresh().catch(e => console.warn(e)));
  }
  if (window.PriceHistoryModule?.refresh) {
    refreshPromises.push(window.PriceHistoryModule.refresh().catch(e => console.warn(e)));
  }
  if (window.ForecastPanelModule?.refresh) {
    refreshPromises.push(window.ForecastPanelModule.refresh().catch(e => console.warn(e)));
  }
  if (window.AuditPanelModule?.refresh) {
    refreshPromises.push(window.AuditPanelModule.refresh().catch(e => console.warn(e)));
  }

  await Promise.all(refreshPromises);

  // Update footer timestamp
  const footerTimeEl = document.getElementById('footer-last-updated');
  if (footerTimeEl) {
    const now = new Date();
    footerTimeEl.textContent = `Last updated: ${now.toLocaleTimeString()} (${now.toLocaleDateString()})`;
  }
};

// Initialize modules on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
  if (window.SystemStatusModule?.init) window.SystemStatusModule.init();
  if (window.OnboardPanelModule?.init) window.OnboardPanelModule.init();
  if (window.InventoryPanelModule?.init) window.InventoryPanelModule.init();
  if (window.PricingPanelModule?.init) window.PricingPanelModule.init();
  if (window.PriceHistoryModule?.init) window.PriceHistoryModule.init();
  if (window.AuditPanelModule?.init) window.AuditPanelModule.init();

  // Initial load
  window.refreshAllPanels();
});
