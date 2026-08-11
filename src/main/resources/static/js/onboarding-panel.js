/**
 * onboarding-panel.js — the "Onboard / Configure SKU" panel: a form for the
 * pricing inputs every PricingStrategy reads (cost, competitor price,
 * current price, days in inventory, brand-positioning min/max guardrails),
 * backed by PUT /api/pricing-inputs/{sku}. Saving also seeds a flat baseline
 * demand history server-side, so a brand-new SKU (one that would otherwise
 * fail RepricingWorkflow's validate() step with no demand history at all) is
 * immediately priceable.
 *
 * A second, smaller control records additional demand points via
 * POST /api/demand/{sku}, so a demo can visibly shift a SKU's trend from
 * STABLE to RISING/FALLING (and watch the Forecast panel's selected strategy
 * change) instead of it being fixed forever by the seed CSV.
 *
 * On success, tracks the SKU in InventoryPanel's shared list so it shows up
 * in Inventory/Pricing/Forecast/Audit immediately, without waiting for a
 * stock adjustment first.
 */

const OnboardingPanel = (() => {

  function fieldValue(id) {
    const value = document.getElementById(id).value.trim();
    return value === "" ? undefined : Number(value);
  }

  function stringFieldValue(id) {
    const value = document.getElementById(id).value.trim();
    return value === "" ? undefined : value;
  }

  function showStatus(message, isError = false) {
    const el = document.getElementById("onboarding-status");
    el.textContent = message;
    el.className = isError ? "form-status form-status-error" : "form-status form-status-ok";
  }

  async function onSaveClick() {
    const sku = document.getElementById("onboarding-sku-input").value.trim();
    if (!sku) {
      showStatus("Enter a SKU first.", true);
      return;
    }

    const fields = {
      name: stringFieldValue("onboarding-name"),
      cost: fieldValue("onboarding-cost"),
      currentPrice: fieldValue("onboarding-current-price"),
      competitorPrice: fieldValue("onboarding-competitor-price"),
      daysInInventory: fieldValue("onboarding-days-in-inventory"),
      minPrice: fieldValue("onboarding-min-price"),
      maxPrice: fieldValue("onboarding-max-price"),
    };

    const saveBtn = document.getElementById("onboarding-save-btn");
    saveBtn.disabled = true;
    Dashboard.beginManualAction();
    try {
      const result = await Api.upsertPricingInputs(sku, fields);
      const label = result.name ? `${result.name} (${sku})` : sku;
      showStatus(
        `Saved ${label}: cost=$${result.cost.toFixed(2)}, competitor=$${result.competitorPrice.toFixed(2)}, ` +
        `current=$${result.currentPrice.toFixed(2)}, daysInInventory=${result.daysInInventory}, ` +
        `minPrice=${result.minPrice > 0 ? "$" + result.minPrice.toFixed(2) : "unset"}, ` +
        `maxPrice=${result.maxPrice > 0 ? "$" + result.maxPrice.toFixed(2) : "unset"}`
      );

      if (window.InventoryPanel) {
        window.InventoryPanel.trackSku(sku);
        await window.InventoryPanel.render();
      }
      if (window.PricingPanel) await window.PricingPanel.render();
      if (window.ForecastPanel) await window.ForecastPanel.render();
    } catch (err) {
      console.error("Failed to save pricing inputs for", sku, err);
      showStatus(`Couldn't save pricing inputs: ${err.message}`, true);
    } finally {
      saveBtn.disabled = false;
      Dashboard.endManualAction();
    }
  }

  async function onRecordDemandClick() {
    const sku = document.getElementById("onboarding-sku-input").value.trim();
    const units = document.getElementById("onboarding-demand-units").value.trim();
    if (!sku || units === "") {
      showStatus("Enter a SKU and a units value to record demand.", true);
      return;
    }

    const btn = document.getElementById("onboarding-record-demand-btn");
    btn.disabled = true;
    Dashboard.beginManualAction();
    try {
      const result = await Api.recordDemand(sku, Number(units), null);
      showStatus(`Recorded demand for ${sku}: ${result.points.length} point(s), trend now ${result.trend}.`);
      document.getElementById("onboarding-demand-units").value = "";
      if (window.ForecastPanel) await window.ForecastPanel.render();
    } catch (err) {
      console.error("Failed to record demand for", sku, err);
      showStatus(`Couldn't record demand: ${err.message}`, true);
    } finally {
      btn.disabled = false;
      Dashboard.endManualAction();
    }
  }

  function wireToolbar() {
    document.getElementById("onboarding-save-btn").addEventListener("click", onSaveClick);
    document.getElementById("onboarding-record-demand-btn").addEventListener("click", onRecordDemandClick);
  }

  return { wireToolbar };
})();

window.OnboardingPanel = OnboardingPanel;
