# Design Document — Milestone 2
## Dynamic Pricing Engine for Multi-Channel DTC Retail
**Group 1 — CSYE6300**

---

## 1. Design Patterns Used

| Pattern | Problem It Solves in This System |
|---|---|
| **Singleton** | Every sales channel needs to read and write the same stock numbers. A single shared `Inventory` instance guarantees there's never a second, out-of-sync copy of stock data floating around the application. |
| **Strategy** | Pricing logic (cost-plus, competitor-aware, inventory-aging) needs to be swappable at runtime based on market conditions, without rewriting the code that calls it. Strategy lets us add a fourth or fifth pricing algorithm later without touching existing code. |
| **Factory Method** | Creating a sales-channel object (Shopify today; own website and social commerce later) involves construction details (API clients, caching config) that calling code shouldn't need to know. The factory isolates that complexity in one place. |
| **Command** | A price change needs to be something we can queue, log, execute, and — critically — undo. Representing "change this price" as an object rather than a direct method call is what makes undo/redo possible. |
| **Decorator** | Every price change must be audit-logged for compliance, but not every command needs to know about logging. Wrapping a command in a logging decorator adds that behavior without touching the command's own logic. |
| **Observer** | When stock changes (a sale, a restock), the system should automatically re-evaluate pricing for that item — without every part of the code that could change stock needing to remember to trigger a reprice. Inventory publishes change events; a repricing observer subscribes to them. |
| **Adapter** | Demand signal data arrives in whatever shape the upstream feed uses (product codes, string dates, order counts), which doesn't match the shape our forecasting logic wants. The adapter translates between the two so the forecaster never has to change when the upstream feed does. |
| **Template Method** | Every repricing run follows the same four steps in the same order: fetch data, validate it, execute the pricing decision, log the outcome. Template Method fixes that sequence in one place while still allowing individual steps to vary. |

---

## 2. System Architecture

The system is a modular Java application (no UI yet — Milestone 3 introduces a REST API and dashboard). At a high level, data flows in one direction through a pipeline, with a feedback loop from inventory back into pricing:

1. **Data ingestion layer.** CSV files (`inventory.csv`, `demand_signals.csv`) represent the current stock levels and historical demand signals a DTC brand would normally get from Shopify, its own site, and social commerce. This layer feeds both the Inventory singleton and the demand-forecasting logic.

2. **Inventory layer.** A single shared Inventory component holds current stock levels for every SKU. It is the system of record that every other component reads from, and it broadcasts an event any time a stock level changes.

3. **Forecasting layer.** The demand forecaster consumes historical demand data (after it's been translated into the system's internal format) and produces a simple trend classification — rising, falling, or stable — using short-term vs. long-term moving averages. No external machine learning service is involved; it's straightforward statistical logic.

4. **Decision layer.** Given a demand trend, a strategy selector picks which pricing algorithm should run for that SKU right now, and that algorithm computes a concrete new price using cost, competitor price, aging, and current stock as inputs.

5. **Execution layer.** The computed price change is wrapped as a command object, decorated with audit logging, and executed. Execution pushes the new price out to every registered sales channel.

6. **Channel layer.** Each sales channel (currently Shopify) is responsible for actually applying and publishing the price through its own API, with local caching so repeated reads don't hit the external API unnecessarily.

7. **Feedback loop.** Whenever the Inventory layer's stock changes, it notifies a listener that automatically kicks off another pass through the Forecasting → Decision → Execution layers for the affected SKU — so a sudden sale or restock is reflected in pricing without a human or a scheduled job needing to trigger it.

**Direction of data flow:** CSV data → Inventory / Demand history → Forecaster → Strategy Selector → Command execution → Sales Channel API → (channel confirms) → Audit log. Inventory changes flow back into the Forecaster/Selector/Command chain via the observer notification.

There is no database yet (CSV files stand in for persistent storage in Milestone 2); no caching layer beyond the per-channel in-memory price cache; and no frontend. All of these are explicitly scoped into Milestone 3 below.

---

## 3. Component / Service Breakdown

**Inventory Service**
Purpose: single source of truth for stock levels across all channels.
Pattern(s): Singleton (one shared instance, so no channel can see stale stock data), Observer (as the "subject" that notifies listeners when stock changes).

**Pricing Strategy Service**
Purpose: computes a concrete price for a SKU given cost, competitor price, aging, and stock.
Pattern(s): Strategy — three interchangeable algorithms behind one interface, so the rest of the system never needs to know which one is active.

**Demand Forecasting Service**
Purpose: classifies recent demand for a SKU as rising, falling, or stable using moving averages.
Pattern(s): none directly, but it depends on the Adapter to receive data in a consistent shape regardless of the upstream feed format.

**Demand Signal Adapter**
Purpose: translates the external/raw shape of demand data into the internal shape the Forecasting Service expects.
Pattern(s): Adapter — isolates the rest of the system from upstream feed formatting changes.

**Pricing Strategy Selector**
Purpose: chooses which pricing strategy should run based on the current demand trend.
Pattern(s): works directly with the Strategy pattern above; this is the "context" that decides which strategy object to hand off to.

**Command & Audit Service**
Purpose: represents a price change as an executable, undoable action, and guarantees every price change is logged for compliance before it's considered complete.
Pattern(s): Command (execute/undo/redo), Decorator (audit logging layered on top of any command without modifying it).

**Channel Management Service**
Purpose: creates and coordinates every sales channel the brand sells through, and fans price updates out to all of them.
Pattern(s): Factory Method (channel creation), with each concrete channel (Shopify, and later own-website/social-commerce) implementing a common interface.

**Shopify Integration Client**
Purpose: talks to Shopify's GraphQL API to read live product/inventory data and push price updates, with a 5-minute cache and graceful fallback if the live call fails.
Pattern(s): none of the eight GoF patterns directly, but it's built behind a swappable interface so the real HTTP-based implementation and an in-memory mock implementation (used for testing) are interchangeable.

**Repricing Workflow (Orchestrator)**
Purpose: ties every other service together into one consistent sequence — fetch demand data, validate it, execute the pricing decision, log the result — every time a reprice needs to happen, whether triggered by a stock change or (in Milestone 3) a schedule.
Pattern(s): Template Method — the four-step sequence is fixed in a base class; only how each step is carried out can vary.

---

## 4. Technology Stack

- **Language:** Java (JDK 21)
- **Build Tool:** Maven 3.8+
- **Testing:** JUnit 5 (Jupiter)
- **Data Source:** CSV files (inventory and demand signals)
- **Logging:** SLF4J + Logback
- **External Integration:** Shopify Admin GraphQL API (mocked for Milestone 2 testing)
- **Planned for Milestone 3:** Spring Boot (REST API layer), a persistent relational store (replacing CSV), and a basic web dashboard

---

## 5. What's Explicitly Out of Scope for Milestone 2

- Persistent database storage (CSV files are the interim data source)
- Any REST API or web-facing interface
- Real (non-mocked) calls to the Shopify API in the automated test suite
- Additional sales channels beyond Shopify
- Brand-positioning price guardrails (min/max bounds)

These are all planned for Milestone 3 and are listed in the updated Milestone document.
