/**
 * System Status Panel & Demo Data Seed Handler
 */

window.SystemStatusModule = {
  init() {
    const seedBtn = document.getElementById('btn-load-demo');
    if (seedBtn) {
      seedBtn.addEventListener('click', () => this.handleSeedDemoData());
    }
    this.refresh();
  },

  async refresh() {
    const dbStatusEl = document.getElementById('status-db');
    const uptimeEl = document.getElementById('status-uptime');
    const scheduledEl = document.getElementById('status-scheduled');

    // 1. Fetch System Status -- real shape from SystemStatusController is
    // { persistenceEnabled: bool, serverStartedAt: ISO timestamp }, not database.connected/uptimeSeconds.
    try {
      const sys = await window.API.getSystemStatus();
      if (sys && typeof sys.persistenceEnabled === 'boolean') {
        if (dbStatusEl) {
          dbStatusEl.innerHTML = sys.persistenceEnabled
            ? `<span class="status-dot online"></span> Persistence: ON`
            : `<span class="status-dot offline"></span> Persistence: OFF`;
        }
      } else if (dbStatusEl) {
        dbStatusEl.innerHTML = `<span class="status-dot offline"></span> Persistence: —`;
      }

      if (uptimeEl) {
        if (sys && sys.serverStartedAt) {
          const uptimeSeconds = Math.max(0, Math.floor((Date.now() - new Date(sys.serverStartedAt).getTime()) / 1000));
          const hrs = Math.floor(uptimeSeconds / 3600);
          const mins = Math.floor((uptimeSeconds % 3600) / 60);
          const secs = uptimeSeconds % 60;
          uptimeEl.textContent = `Uptime: ${hrs}h ${mins}m ${secs}s`;
        } else {
          uptimeEl.textContent = `Uptime: —`;
        }
      }
    } catch (err) {
      if (dbStatusEl) dbStatusEl.innerHTML = `<span class="status-dot offline"></span> Persistence: —`;
      if (uptimeEl) uptimeEl.textContent = `Uptime: —`;
    }

    // 2. Fetch Scheduled Status -- real shape is { active, skus, lastRunAt, nextRunAt };
    // there's no intervalHours field, so don't fabricate one.
    try {
      const sched = await window.API.getScheduledStatus();
      if (scheduledEl) {
        if (sched && sched.active) {
          const lastRun = sched.lastRunAt ? new Date(sched.lastRunAt).toLocaleTimeString() : 'never';
          const nextRun = sched.nextRunAt ? new Date(sched.nextRunAt).toLocaleTimeString() : '—';
          scheduledEl.textContent = `Scheduled Job: ON · last run ${lastRun} · next run ${nextRun}`;
        } else if (sched) {
          scheduledEl.textContent = `Scheduled Job: OFF`;
        } else {
          scheduledEl.textContent = `Scheduled Job: —`;
        }
      }
    } catch (err) {
      if (scheduledEl) scheduledEl.textContent = `Scheduled Job: —`;
    }
  },

  async handleSeedDemoData() {
    const seedBtn = document.getElementById('btn-load-demo');
    if (seedBtn) {
      seedBtn.disabled = true;
      seedBtn.textContent = 'Seeding...';
    }

    try {
      const res = await window.API.seedDemoData();
      if (res && Array.isArray(res.seeded)) {
        res.seeded.forEach(sku => window.TrackedSkus.add(sku));
      }
      if (window.refreshAllPanels) {
        await window.refreshAllPanels();
      }
    } catch (err) {
      console.error('[SystemStatus] Seed failed:', err);
      alert('Failed to load demo data from API.');
    } finally {
      if (seedBtn) {
        seedBtn.disabled = false;
        seedBtn.textContent = 'Load Demo Data';
      }
    }
  }
};
