/**
 * Panel 1 — Onboard / Configure SKU
 */

window.OnboardPanelModule = {
  init() {
    const saveInputsBtn = document.getElementById('btn-save-inputs');
    if (saveInputsBtn) {
      saveInputsBtn.addEventListener('click', (e) => {
        e.preventDefault();
        this.handleSaveInputs();
      });
    }

    const recordDemandBtn = document.getElementById('btn-record-demand');
    if (recordDemandBtn) {
      recordDemandBtn.addEventListener('click', (e) => {
        e.preventDefault();
        this.handleRecordDemand();
      });
    }
  },

  async handleSaveInputs() {
    const sku = document.getElementById('input-sku')?.value?.trim();
    const statusEl = document.getElementById('onboard-status-msg');

    if (!sku) {
      this.showStatus('Please enter a valid SKU name.', 'error');
      return;
    }

    const inputs = {
      cost: document.getElementById('input-cost')?.value,
      currentPrice: document.getElementById('input-current-price')?.value,
      competitorPrice: document.getElementById('input-competitor-price')?.value,
      daysInInventory: document.getElementById('input-days-inventory')?.value,
      minPrice: document.getElementById('input-min-price')?.value,
      maxPrice: document.getElementById('input-max-price')?.value
    };

    const saveBtn = document.getElementById('btn-save-inputs');
    if (saveBtn) saveBtn.disabled = true;
    this.showStatus('Saving pricing inputs...', 'info');

    try {
      await window.API.savePricingInputs(sku, inputs);
      window.TrackedSkus.add(sku);
      this.showStatus(`Successfully saved pricing inputs for ${sku}.`, 'success');
      if (window.refreshAllPanels) window.refreshAllPanels();
    } catch (err) {
      const msg = err.message || 'Failed to save pricing inputs';
      this.showStatus(`Error saving inputs for ${sku}: ${msg}`, 'error');
    } finally {
      if (saveBtn) saveBtn.disabled = false;
    }
  },

  async handleRecordDemand() {
    const sku = document.getElementById('demand-sku')?.value?.trim() || document.getElementById('input-sku')?.value?.trim();
    const units = document.getElementById('demand-units')?.value;

    if (!sku) {
      this.showStatus('Please enter a SKU to record demand.', 'error');
      return;
    }
    if (units === undefined || units === null || units === '') {
      this.showStatus('Please enter units sold today.', 'error');
      return;
    }

    const recordBtn = document.getElementById('btn-record-demand');
    if (recordBtn) recordBtn.disabled = true;
    this.showStatus('Recording demand...', 'info');

    try {
      const res = await window.API.recordDemand(sku, units);
      window.TrackedSkus.add(sku);
      const trend = res.trend || (res.data && res.data.trend) || 'UPDATED';
      const detail = res.message || `Recorded ${units} units for ${sku}. Trend: ${trend}`;
      this.showStatus(detail, 'success');
      if (window.refreshAllPanels) window.refreshAllPanels();
    } catch (err) {
      const msg = err.message || 'Failed to record demand';
      this.showStatus(`Error recording demand for ${sku}: ${msg}`, 'error');
    } finally {
      if (recordBtn) recordBtn.disabled = false;
    }
  },

  showStatus(message, type = 'info') {
    const statusEl = document.getElementById('onboard-status-msg');
    if (!statusEl) return;
    statusEl.className = `status-msg ${type}`;
    statusEl.style.display = 'flex';
    statusEl.innerHTML = `<span>${message}</span>`;
  }
};
