/**
 * API Client Module — Single point of truth for constructing relative endpoint paths.
 * Served from the same origin as Spring Boot REST API.
 */

window.API = {
  /**
   * Helper function to execute fetch calls and handle errors
   */
  async request(path, options = {}) {
    try {
      const response = await fetch(path, {
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          ...(options.headers || {})
        },
        ...options
      });

      if (!response.ok) {
        const errorText = await response.text().catch(() => '');
        const errorObj = {
          status: response.status,
          statusText: response.statusText,
          message: errorText || `HTTP ${response.status} ${response.statusText}`
        };
        throw errorObj;
      }

      // Return parsed JSON if response has content
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }
      return await response.text();
    } catch (err) {
      console.warn(`[API] Request failed for path "${path}":`, err);
      throw err;
    }
  },

  // GET /api/system/status
  getSystemStatus() {
    return this.request('/api/system/status');
  },

  // GET /api/pricing/scheduled-status
  getScheduledStatus() {
    return this.request('/api/pricing/scheduled-status');
  },

  // POST /api/demo/seed
  seedDemoData() {
    return this.request('/api/demo/seed', { method: 'POST' });
  },

  // GET /api/inventory
  getInventory() {
    return this.request('/api/inventory');
  },

  // POST /api/inventory/{sku}/adjust?delta=±5
  adjustInventory(sku, delta) {
    const encodedSku = encodeURIComponent(sku);
    return this.request(`/api/inventory/${encodedSku}/adjust?delta=${delta}`, { method: 'POST' });
  },

  // GET /api/pricing/{sku}
  getPricing(sku) {
    const encodedSku = encodeURIComponent(sku);
    return this.request(`/api/pricing/${encodedSku}`);
  },

  // POST /api/pricing/{sku}/reprice
  repriceSku(sku) {
    const encodedSku = encodeURIComponent(sku);
    return this.request(`/api/pricing/${encodedSku}/reprice`, { method: 'POST' });
  },

  // POST /api/pricing/undo
  undoLastCommand() {
    return this.request('/api/pricing/undo', { method: 'POST' });
  },

  // GET /api/pricing-inputs/{sku}
  getPricingInputs(sku) {
    const encodedSku = encodeURIComponent(sku);
    return this.request(`/api/pricing-inputs/${encodedSku}`);
  },

  // PUT /api/pricing-inputs/{sku}?field=val...
  savePricingInputs(sku, inputParams = {}) {
    const encodedSku = encodeURIComponent(sku);
    const searchParams = new URLSearchParams();

    Object.entries(inputParams).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        searchParams.append(key, val);
      }
    });

    const queryString = searchParams.toString();
    const url = `/api/pricing-inputs/${encodedSku}${queryString ? '?' + queryString : ''}`;
    return this.request(url, { method: 'PUT' });
  },

  // POST /api/demand/{sku}?units=N
  recordDemand(sku, units) {
    const encodedSku = encodeURIComponent(sku);
    return this.request(`/api/demand/${encodedSku}?units=${encodeURIComponent(units)}`, { method: 'POST' });
  },

  // GET /api/pricing/{sku}/forecast
  getForecast(sku) {
    const encodedSku = encodeURIComponent(sku);
    return this.request(`/api/pricing/${encodedSku}/forecast`);
  },

  // GET /api/audit OR GET /api/audit/{sku}
  getAuditLogs(sku = null) {
    if (sku && sku !== 'ALL') {
      return this.request(`/api/audit/${encodeURIComponent(sku)}`);
    }
    return this.request('/api/audit');
  }
};
