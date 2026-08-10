# Web Dashboard — File Plan
## Dynamic Pricing Engine for Multi-Channel DTC Retail — Group 1

This document lists every file needed to add a basic web dashboard on top of the existing Spring Boot REST API, and what each file is responsible for. Matches the "basic web dashboard" item already scoped in the Milestone 3 doc under `shopify-integration-dashboard` (owner: Sai Vinayaka Venkata Prateek Kacham).

**Approach:** static HTML/CSS/JS served directly by Spring Boot from `src/main/resources/static/` — no separate frontend build, no new framework, calls the existing `InventoryController`, `PricingController`, and `AuditController` endpoints with `fetch()`.

---

## 1. File List & Responsibilities

```
src/main/resources/static/
 ├─ index.html
 ├─ css/
 │   └─ dashboard.css
 └─ js/
     ├─ api.js
     ├─ inventory-panel.js
     ├─ pricing-panel.js
     ├─ audit-panel.js
     └─ dashboard.js
```

### `index.html`
The single page shell. Holds the layout structure — a header, three panel containers (Inventory, Pricing, Audit Log), and script/style tags. Contains no logic itself; every panel is populated by JS at load time. This is the file a user actually opens in the browser (`http://localhost:8080/`).

### `css/dashboard.css`
All visual styling: layout grid for the three panels, table styling for stock/price/audit rows, color coding for demand trend (e.g. green for RISING, red for FALLING, gray for STABLE), and basic responsive behavior so it's usable on a laptop screen during a demo.

### `js/api.js`
The only file that knows the actual endpoint URLs. Wraps every backend call in a small function so the rest of the JS never constructs a URL by hand:
- `getInventory(sku)` → `GET /api/inventory/{sku}`
- `adjustStock(sku, delta)` → `POST /api/inventory/{sku}/adjust`
- `reprice(sku)` → `POST /api/pricing/{sku}/reprice`
- `getPrices(sku)` → `GET /api/pricing/{sku}`
- `getAuditReport()` → `GET /api/audit`

If any endpoint's path or payload shape changes later, this is the only file that needs to change.

### `js/inventory-panel.js`
Renders the Inventory panel: current stock per SKU, and a small form/button to adjust stock (simulating a sale or restock). Calls `api.js`'s `getInventory` and `adjustStock`. After an adjustment, re-fetches and re-renders so the dashboard reflects the Observer-triggered reprice that just happened on the backend.

### `js/pricing-panel.js`
Renders the Pricing panel: current price per SKU per channel, and a "Reprice now" button per SKU that calls `api.js`'s `reprice`. Displays the resulting new price immediately after the call returns.

### `js/audit-panel.js`
Renders the Audit Log panel: a table of every logged command from `AuditLoggingCommandDecorator`'s report (timestamp, SKU, action, original price, new price). Calls `api.js`'s `getAuditReport`. Includes a manual "Refresh" button since this panel is a log, not something the user edits.

### `js/dashboard.js`
The entry point. Runs on page load: calls each panel's render function once, sets up a periodic refresh (e.g. every 10 seconds) so the dashboard stays current without a manual reload, and wires up any shared UI behavior (like a global loading indicator or error banner if the API is unreachable).

---

## 2. Backend Changes Needed (if any)

The dashboard should work against your existing controllers with no backend changes, **except**:

- [ ] Confirm `InventoryController` exposes a `GET /api/inventory/{sku}` (read) endpoint — the Milestone 3 doc only explicitly lists "view and adjust stock levels," so double-check both directions exist.
- [ ] Confirm `AuditController`'s `/api/audit` endpoint returns JSON (not plain text) so `audit-panel.js` can parse and render it as a table.
- [ ] If the frontend and backend end up on different ports/origins during local dev, add CORS config — not needed if served from the same Spring Boot app under `static/`.

---

## 3. Suggested Build Order

1. `api.js` — get every endpoint call working and log responses to the browser console before writing any rendering code.
2. `index.html` + `dashboard.css` — static shell and layout, no live data yet.
3. `inventory-panel.js` — first live panel, since it's the simplest read/write.
4. `pricing-panel.js` — depends on Inventory panel's stock changes triggering a visible reprice.
5. `audit-panel.js` — last, since it's just a read-only table.
6. `dashboard.js` — wire everything together, add the refresh interval last once each panel works independently.

---

## 4. Out of Scope for This Dashboard

- User authentication/login
- Editing pricing strategy parameters from the UI (still config/code-only)
- Charting library for demand trend visualization (a plain trend label — RISING/FALLING/STABLE — is sufficient for "basic")
- Mobile-optimized layout
