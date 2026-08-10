/**
 * dashboard.js — the entry point. Runs once the page loads: renders every
 * panel, wires up each panel's toolbar interactions, and sets a periodic
 * refresh so the dashboard stays current without a manual reload. Also owns
 * the shared error banner so any panel can surface a backend problem
 * without duplicating that UI itself.
 */

const Dashboard = (() => {
  const REFRESH_INTERVAL_MS = 10000;
  let refreshTimer = null;

  // Counts manual actions (reprice, adjust) currently in flight. While >0,
  // the periodic auto-refresh skips itself instead of racing a manual
  // click's own render() call, which would destroy/recreate the button the
  // user just clicked (disabled + "Repricing…") mid-request.
  let manualActionsInFlight = 0;

  function beginManualAction() {
    manualActionsInFlight++;
  }

  function endManualAction() {
    manualActionsInFlight = Math.max(0, manualActionsInFlight - 1);
  }

  function showError(message) {
    const banner = document.getElementById("status-banner");
    banner.textContent = message;
    banner.hidden = false;
    banner.className = "status-banner status-error";
  }

  function clearBanner() {
    const banner = document.getElementById("status-banner");
    banner.hidden = true;
  }

  function setLastUpdated() {
    document.getElementById("last-updated").textContent =
      `Last updated: ${new Date().toLocaleTimeString()}`;
  }

  async function refreshAll() {
    if (manualActionsInFlight > 0) {
      // A manual reprice/adjust is running its own render() right now --
      // skip this tick rather than racing it and clobbering the button the
      // user just clicked. The next interval tick will pick up any changes.
      return;
    }

    clearBanner();
    // PricingPanel reads InventoryPanel's tracked-SKU list, so Inventory
    // must finish rendering (and populating that list) before Pricing
    // renders. Audit is independent and can run alongside either.
    await InventoryPanel.render();
    await Promise.all([
      PricingPanel.render(),
      AuditPanel.render(),
    ]);
    setLastUpdated();
  }

  function startAutoRefresh() {
    if (refreshTimer) clearInterval(refreshTimer);
    refreshTimer = setInterval(refreshAll, REFRESH_INTERVAL_MS);
  }

  async function init() {
    InventoryPanel.wireToolbar();
    AuditPanel.wireToolbar();
    await refreshAll();
    startAutoRefresh();
  }

  return { showError, init, beginManualAction, endManualAction };
})();

window.Dashboard = Dashboard;

document.addEventListener("DOMContentLoaded", () => {
  Dashboard.init();
});
