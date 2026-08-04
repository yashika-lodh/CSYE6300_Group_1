# Design Document — Milestone 3
## Dynamic Pricing Engine for Multi-Channel DTC Retail
**Group 1 — CSYE6300**

---

## 1. Design Patterns Used

| Pattern | Problem It Solves in This System |
|---|---|
| **Singleton** | Every sales channel needs to read and write the same stock numbers. A single shared `Inventory` instance guarantees there's never a second, out-of-sync copy of stock data floating around the application. In Milestone 3 this instance is also seeded from, and kept in sync with, a persistent database — but there is still only ever one in-memory Inventory. |
| **Strategy** | Pricing logic (cost-plus, competitor-aware, inventory-aging) needs to be swappable at runtime based on market conditions, without rewriting the code that calls it. New in Milestone 3: each strategy's output can now be clamped to a brand-positioning floor and/or ceiling, without any strategy needing to know about the others or duplicate that clamping logic itself. |
| **Factory Method** | Creating a sales-channel object involves construction details (API clients, caching config) that calling code shouldn't need to know. New in Milestone 3: two additional channel types (own website, social commerce) are created through the same factory, with zero changes required anywhere that already depended on the factory. |
| **Command** | A price change needs to be something we can queue, log, execute, and — critically — undo. Representing "change this price" as an object rather than a direct method call is what makes undo/redo possible. Unchanged since Milestone 2. |
| **Decorator** | Every price change must be audit-logged for compliance, but not every command needs to know about logging. New in Milestone 3: the audit trail is now also maintained as a static, cross-command log, so a single system-wide audit report can be produced instead of one log per command instance. |
| **Observer** | When stock changes, the system should automatically re-evaluate pricing for that item without every part of the code that could change stock needing to remember to trigger a reprice. New in Milestone 3: a second, independent observer writes every stock change through to the database, so the persistence layer and the repricing trigger are two separate subscribers reacting to the same event, neither aware of the other. |
| **Adapter** | Demand signal data arrives in whatever shape the upstream feed uses, which doesn't match the shape the forecasting logic wants. The adapter translates between the two so the forecaster never has to change when the upstream feed does. Unchanged since Milestone 2. |
| **Template Method** | Every repricing run follows the same four steps in the same order: fetch data, validate it, execute the pricing decision, log the outcome. New in Milestone 3: a second concrete workflow reuses that exact fixed sequence but is triggered by a timer instead of a stock-change event, proving the sequence really is independent of what triggers it. |
| **Repository** *(non-GoF, standard persistence pattern)* | New in Milestone 3. Two repositories isolate all database/file-reading detail behind small, testable interfaces: one persists stock levels to a real database (replacing the Milestone 2 CSV file), and one reads historical demand signals from CSV. Neither the Inventory singleton nor the forecasting logic needs to know how or where that data is actually stored. |

---

## 2. System Architecture

The system is still a modular Java application built around one core pricing pipeline, but Milestone 3 wraps that pipeline in a web-facing layer and gives it durable storage:

1. **Presentation layer (new in Milestone 3).** A small static dashboard, served by the application itself, gives a human a live read-only view of inventory, per-channel prices, and the audit trail. It talks to nothing but the REST API below — it has no direct access to any internal component.

2. **API layer (new in Milestone 3).** A REST API sits in front of the engine, exposing three areas of functionality: viewing and adjusting inventory, triggering a repricing cycle and reading current prices, and retrieving the audit report. The API layer does not contain business logic itself — it is a thin façade that wires HTTP requests to the same internal components the Milestone 2 console demo used directly.

3. **Data ingestion layer.** Historical demand signals are now read from a CSV file through a dedicated repository component rather than being loaded ad hoc, with a fallback to sample data if that file is unavailable. Stock levels, previously CSV-based, are now read from and written to a real persistent database at startup and on every change.

4. **Inventory layer.** A single shared Inventory component still holds the current, fast, in-memory stock levels that everything else reads from, and still broadcasts an event any time a stock level changes. What differs in Milestone 3 is that this component is seeded from the database when the application starts, and every change it broadcasts is now picked up by two independent listeners instead of one.

5. **Forecasting layer.** Unchanged in behavior: historical demand data, translated into the system's internal format, produces a simple trend classification (rising, falling, stable) using moving averages.

6. **Decision layer.** Given a demand trend, a strategy selector picks which pricing algorithm should run, and that algorithm computes a concrete new price from cost, competitor price, aging, and stock. New in Milestone 3: the computed price is passed through a brand-positioning guardrail check before it is considered final, so no strategy can produce a price outside limits the business has configured.

7. **Execution layer.** The computed price change is wrapped as a command object, decorated with audit logging, and executed, pushing the new price out to every registered sales channel. The audit trail produced here is now aggregated system-wide rather than per command, so it can be reported on as a whole.

8. **Channel layer.** Each sales channel is responsible for actually applying and publishing a price. Shopify's channel now talks to a real external HTTP endpoint when credentials are configured, falling back to an in-memory mock automatically when they are not. Two additional channels (own website, social commerce) exist behind the same interface and currently keep their prices in memory, since neither has an external system to publish to yet.

9. **Persistence layer (new in Milestone 3).** A relational database, accessed through a repository component, durably stores stock levels so they survive an application restart — something CSV files in Milestone 2 could not do within a single running process, and could not do safely across concurrent access at all.

10. **Feedback loop.** Unchanged in principle: whenever the Inventory layer's stock changes, it notifies listeners that kick off another pass through the Forecasting → Decision → Execution chain for the affected SKU. Milestone 3 adds a second, independent trigger for that same chain — a fixed-schedule sweep across a configured set of SKUs — so repricing can now happen either reactively (stock changed) or proactively (time elapsed), through the exact same underlying pipeline either way.

**Direction of data flow:** Dashboard → REST API → Inventory / Demand history (now database- and CSV-repository-backed) → Forecaster → Strategy Selector → Guardrail check → Command execution → Sales Channel(s) → Audit log → REST API (audit endpoint) → Dashboard. Inventory changes flow back into both the Forecaster/Selector/Command chain (via the repricing observer) and the database (via the persistence observer), independently of each other. A scheduled timer can also enter this same chain directly, bypassing the need for a stock-change event.

There is still no caching layer beyond the per-channel in-memory price cache, and no authentication on the API — both are noted as open items for Final Submission, along with real-network Shopify calls remaining behind a credential-gated integration test rather than the default test suite.

---

## 3. Component / Service Breakdown

**Inventory Service**
Purpose: single source of truth for in-memory stock levels across all channels, now backed by durable storage.
Pattern(s): Singleton (one shared instance), Observer (the "subject" that notifies listeners — now two of them — when stock changes).

**Inventory Persistence Service**
Purpose: writes every stock change through to a relational database, and seeds the in-memory Inventory from that database at startup, so stock survives an application restart.
Pattern(s): Observer (a subscriber to Inventory's change events), Repository (isolates all database access behind a small interface).

**Pricing Strategy Service**
Purpose: computes a concrete price for a SKU given cost, competitor price, aging, and stock, then enforces any configured brand-positioning floor/ceiling on the result.
Pattern(s): Strategy — three interchangeable algorithms behind one interface, each routing its output through the same guardrail check so limits apply uniformly regardless of which algorithm is active.

**Demand Forecasting Service**
Purpose: classifies recent demand for a SKU as rising, falling, or stable using moving averages.
Pattern(s): none directly, but depends on the Adapter to receive data in a consistent shape, and on the new Demand Signal Repository to source that data from a real file instead of hardcoded samples.

**Demand Signal Adapter**
Purpose: translates the external/raw shape of demand data into the internal shape the Forecasting Service expects.
Pattern(s): Adapter — isolates the rest of the system from upstream feed formatting changes. Unchanged since Milestone 2.

**Demand Signal Repository**
Purpose: loads historical demand history from a CSV file, falling back to sample data only if that file is unavailable.
Pattern(s): Repository — isolates file-reading detail from the Forecasting Service.

**Pricing Strategy Selector**
Purpose: chooses which pricing strategy should run based on the current demand trend.
Pattern(s): works directly with the Strategy pattern above, as the "context" that decides which strategy object to hand off to.

**Command & Audit Service**
Purpose: represents a price change as an executable, undoable action, and guarantees every price change is logged for compliance — now as part of one system-wide audit trail rather than one log per command.
Pattern(s): Command (execute/undo/redo), Decorator (audit logging layered on top of any command without modifying it).

**Channel Management Service**
Purpose: creates and coordinates every sales channel the brand sells through, and fans price updates out to all of them.
Pattern(s): Factory Method (channel creation for all three channel types), with each concrete channel implementing a common interface so the manager and every caller stay unchanged as new channel types are added.

**Shopify Integration Client**
Purpose: talks to Shopify's GraphQL API to read live product/inventory data and push price updates, with a 5-minute cache and graceful fallback if the live call fails. Now capable of a real HTTP-based connection to a Shopify dev store, in addition to the in-memory mock used for the default test suite.
Pattern(s): none of the nine patterns above directly, but built behind a swappable interface so the mock and the real HTTP implementation are interchangeable at construction time based on whether live credentials are configured.

**Own-Website & Social-Commerce Channels**
Purpose: represent two additional sales surfaces the brand controls, each able to receive a price and (eventually) publish it externally.
Pattern(s): implement the same Factory-Method-created SalesChannel interface as the Shopify channel; neither yet has a real external system to publish to, so each currently confirms the price is set rather than pushing it out over a network.

**Repricing Workflow (Orchestrator)**
Purpose: ties every other service together into one consistent sequence — fetch demand data, validate it, execute the pricing decision, log the result — every time a reprice needs to happen, whether triggered by a stock change or, new in Milestone 3, a fixed schedule.
Pattern(s): Template Method — the four-step sequence is fixed in a base class; a second concrete subclass reuses that sequence unchanged while adding its own timer-based trigger.

**REST API Layer**
Purpose: exposes inventory management, repricing, and audit reporting over HTTP, so the engine can be driven and observed without the console demo.
Pattern(s): none of the nine patterns directly — this is a thin façade wired to the services above, not a new business-logic component.

**Dashboard**
Purpose: gives a human a live, read-only view of inventory, per-channel prices, and the audit trail, built entirely on top of the REST API layer.
Pattern(s): none — a static presentation layer with no direct access to any internal component.

---

## 4. Technology Stack

- **Language:** Java (JDK 21)
- **Build Tool:** Maven 3.9+
- **Frameworks:** Spring Boot (REST API layer, new in Milestone 3)
- **Persistence:** Hibernate/JPA + H2 (file-backed relational database, new in Milestone 3 — replaces the Milestone 2 CSV file for stock levels)
- **Data Source (demand history):** CSV file, now read through a dedicated repository component rather than hardcoded sample data
- **Testing:** JUnit 5 (Jupiter), Spring Boot Test, with a separate credential-gated integration test for real Shopify network calls kept out of the default suite
- **Logging:** SLF4J + Logback
- **External Integration:** Shopify Admin GraphQL API, with a real HTTP-based client now available alongside the Milestone 2 mock, auto-selected based on whether live credentials are configured
- **Version Control:** Git + GitHub (private repository)

---

## 5. What's Explicitly Out of Scope for Milestone 3

- Authentication/authorization on the REST API endpoints
- A caching layer beyond the existing per-channel in-memory price cache
- Deployment of the database to a persistent, shared instance beyond a local file
- Write actions (price overrides, guardrail configuration) from the dashboard, which is currently read-only
- Real Shopify network calls as part of the default, credential-free test suite (kept as a separate, opt-in integration test)

These are open discussion points for Final Submission scope, to be confirmed with the team, and are listed alongside the updated Milestone document.
