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

  /** Fetched once on load, not on the 10s refresh -- persistence and server start time never change while the page is open. */
  async function loadSystemStatus() {
    const line = document.getElementById("system-status-line");
    try {
      const status = await Api.getSystemStatus();
      const persistence = status.persistenceEnabled ? "ON" : "OFF";
      const startedAt = new Date(status.serverStartedAt).toLocaleString();
      line.textContent = `Persistence: ${persistence} · Server running since ${startedAt}`;
    } catch (err) {
      console.error("Failed to load system status", err);
      line.textContent = "Persistence: unknown · Server status unavailable";
    }
  }

  /** Refreshed every 10s cycle, unlike system status -- the sweep's lastRunAt/nextRunAt change over time. */
  async function loadScheduledStatus() {
    const line = document.getElementById("scheduled-status-line");
    try {
      const status = await Api.getScheduledStatus();
      const state = status.active ? "ON" : "OFF";
      const skuList = status.skus && status.skus.length ? status.skus.join(", ") : "no SKUs";
      const lastRun = status.lastRunAt ? new Date(status.lastRunAt).toLocaleTimeString() : "never";
      const nextRun = status.nextRunAt ? new Date(status.nextRunAt).toLocaleTimeString() : "—";
      line.textContent = `Scheduled repricing: ${state} (${skuList}) · last run ${lastRun} · next run ${nextRun}`;
    } catch (err) {
      console.error("Failed to load scheduled status", err);
      line.textContent = "Scheduled repricing: unavailable";
    }
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
      GuardrailsPanel.render(),
      ForecastPanel.render(),
      AuditPanel.render(),
      PriceHistoryPanel.render(),
      loadScheduledStatus(),
    ]);
    setLastUpdated();
  }

  function startAutoRefresh() {
    if (refreshTimer) clearInterval(refreshTimer);
    refreshTimer = setInterval(refreshAll, REFRESH_INTERVAL_MS);
  }

  async function onSeedDemoDataClick(event) {
    const btn = event.currentTarget;
    btn.disabled = true;
    btn.textContent = "Loading demo data…";
    beginManualAction();
    try {
      // Real staggered reprices server-side (see DemoDataSeeder) -- this is
      // expected to take a few seconds, not a stalled request.
      const result = await Api.seedDemoData();
      result.seeded.forEach((sku) => InventoryPanel.trackSku(sku));
      await refreshAll();
    } catch (err) {
      console.error("Failed to load demo data", err);
      showError(`Couldn't load demo data: ${err.message}`);
    } finally {
      btn.disabled = false;
      btn.textContent = "Load Demo Data";
      endManualAction();
    }
  }

  async function init() {
    InventoryPanel.wireToolbar();
    OnboardingPanel.wireToolbar();
    AuditPanel.wireToolbar();
    PriceHistoryPanel.wireToolbar();
    document.getElementById("seed-demo-data-btn").addEventListener("click", onSeedDemoDataClick);
    await loadSystemStatus();
    await refreshAll();
    startAutoRefresh();
  }

  return { showError, init, beginManualAction, endManualAction, refreshAll };
})();

window.Dashboard = Dashboard;

document.addEventListener("DOMContentLoaded", () => {
  Dashboard.init();
});
