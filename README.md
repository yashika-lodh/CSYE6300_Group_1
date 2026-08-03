# CSYE 6300 - Final Project Milestone 3

## 1. Project Group Number/Name
Group 1

## 2. Project Topic/Name
**Dynamic Pricing Engine for Multi-Channel DTC Retail**

## 3. Problem Statement
Direct-to-consumer (DTC) brands operating across multiple sales channels (Shopify, own website, social commerce) struggle with manual, inconsistent pricing that doesn't adapt to market demand. Our project addresses this by building a unified Dynamic Pricing Engine that forecasts sales demand in real-time, automatically selects the optimal pricing strategy based on market conditions, and synchronizes prices across channels with full audit logging. The system demonstrates how Gang of Four design patterns solve real-world inventory and pricing problems while incorporating lightweight machine learning for demand-driven decision making. By combining predictive analytics with proven design patterns, we create a modular, extensible platform that DTC brands can use to optimize revenue while respecting brand positioning constraints.

## 4. UML Diagram

![UML Diagram](media/uml_diagram.png)

## 5. Design Patterns Implemented

| Pattern | Type | Used For |
|---|---|---|
| Singleton | Creational | Single shared `Inventory` instance across all channels; unchanged since Milestone 2, now also seeded from/synced with the DB |
| Strategy | Behavioral | Interchangeable pricing algorithms (cost-plus, competitor-aware, inventory-aging) |
| Factory Method | Creational | Creating different channel implementations (Shopify, etc.) via `SalesChannelFactory` |
| Command | Behavioral | Encapsulating price-change requests with undo/redo support (`PricingCommand`, `PricingCommandInvoker`) |
| Decorator | Structural | Adding audit logging to pricing commands dynamically (`AuditLoggingCommandDecorator`); extended in Milestone 3 with a static, cross-command audit trail for the `/api/audit` endpoint |
| Observer | Behavioral | Triggering repricing when inventory levels change (`InventoryObserver`, `RepricingTriggerObserver`); extended in Milestone 3 with a second observer, `InventoryPersistenceObserver`, for write-through persistence |
| Adapter | Structural | Adapting demand signal data to the domain model (`DemandSignalAdapter`) |
| Template Method | Behavioral | Defining the repricing workflow skeleton: fetch → validate → execute → log (`RepricingWorkflow`); extended in Milestone 3 with a second concrete subclass, `ScheduledRepricingWorkflow` |
| Repository (non-GoF, standard persistence pattern) | — | New in Milestone 3 — `InventoryRepository` isolates JPA/Hibernate details behind a small, testable interface |

## 6. Tech Stack
- **Language**: Java (JDK 21)
- **Build Tool**: Maven 3.9+
- **Frameworks**: Spring Boot 3.3 (REST API layer, new in Milestone 3)
- **Persistence**: Hibernate/JPA + H2 (file-backed database, new in Milestone 3 — replaces the Milestone 2 CSV files for inventory)
- **IDE**: Eclipse or VS Code with Java extensions
- **Version Control**: Git + GitHub (private repo)
- **Testing**: JUnit 5 (Jupiter), Spring Boot Test (`@SpringBootTest` + `TestRestTemplate` for API integration tests)
- **Logging**: SLF4J + Logback
- **External Integration**: Shopify Admin GraphQL API via `ShopifyApiClient`. Defaults to `MockGraphQLExecutor` for credential-free testing; automatically switches to the real `HttpGraphQLExecutor` transport when `SHOPIFY_SHOP_DOMAIN` and `SHOPIFY_ACCESS_TOKEN` environment variables are set

## 7. Functionalities Implemented for Milestone 3

### Persistent Storage (replaces CSV)
- [x] `InventoryEntity` / `InventoryRepository` — JPA/Hibernate entities and DAO-style repository backed by a file-based H2 database (`data/pricingdb`), configured via `application.properties` + `META-INF/persistence.xml`
- [x] The Inventory Singleton keeps its fast in-memory stock map (unchanged from Milestone 2) but is now seeded from the database at startup and kept in sync via `InventoryPersistenceObserver`, which writes every stock change through to the DB. Stock levels now survive an application restart

### Scheduled / Automated Repricing
- [x] `ScheduledRepricingWorkflow` — a second concrete Template Method workflow (extends `StandardRepricingWorkflow`) that reprices a configured set of SKUs on a fixed timer via `ScheduledExecutorService`, independent of any Observer-triggered stock change

### Spring Boot REST API Layer
- [x] `PricingEngineApplication` — Spring Boot entry point wiring up the exact same engine graph as the console demo (Singleton Inventory, Factory-built channels, Strategy/Selector, Command/Decorator, Template Method workflow, Observer)
- [x] `InventoryController` (`/api/inventory`) — view and adjust stock levels over HTTP
- [x] `PricingController` (`/api/pricing`) — trigger a repricing cycle and read current per-channel prices
- [x] `AuditController` (`/api/audit`) — audit-report endpoint exposing the Decorator's full compliance log, across every command the engine has ever run

### Real Shopify Integration & Dashboard
- [x] `HttpGraphQLExecutor` — real, non-mocked `GraphQLExecutor` implementation that sends queries/mutations to Shopify's Admin GraphQL API over HTTPS
- [x] `ShopifyApiClient` now picks `HttpGraphQLExecutor` or `MockGraphQLExecutor` automatically based on whether `SHOPIFY_SHOP_DOMAIN` / `SHOPIFY_ACCESS_TOKEN` are set (feature flag, zero changes needed at call sites)
- [x] `ShopifyApiClientIntegrationTest` — real network test against a Shopify dev store, skipped by default so `mvn test` stays fast and credential-free for every teammate and for grading
- [x] `DemandSignalRepository` — loads real demand history from `data/demand_signals.csv` (previously present but never read); `PricingEngineApplication` now uses it, falling back to Milestone 2 sample data only if the CSV is missing
- [x] `dashboard.html` — lightweight static dashboard (served at `/dashboard.html`) showing live inventory, per-channel prices, and the audit trail via the existing REST endpoints

## 8. Functionalities Planned for Final Submission
- [ ] Additional SalesChannel implementations — own website and social commerce — behind the existing Factory Method (in progress on branch `channels-integration`)
- [ ] Brand-positioning guardrails — min/max price bounds enforced in every `PricingStrategy` (in progress on branch `channels-integration`)

## 9. Build & Run

```bash
mvn compile
mvn test

# Plain-Java console demo (Singleton/Observer/Command/etc., unchanged from Milestone 2)
mvn compile exec:java -Dexec.mainClass="com.csye6300.group1.pricingengine.Main"

# Milestone 3: Spring Boot REST API (http://localhost:8080)
mvn spring-boot:run
```

### Shopify Real-Transport & Dashboard (Milestone 3)

By default the app runs against `MockGraphQLExecutor` (no credentials needed).
To point it at a real Shopify dev store instead, set these environment
variables before running:

- `SHOPIFY_SHOP_DOMAIN` — e.g. `your-dev-store.myshopify.com`
- `SHOPIFY_ACCESS_TOKEN` — your Shopify Admin API access token

With both set, `ShopifyApiClient` automatically uses `HttpGraphQLExecutor`
instead of the mock.

With the Spring Boot app running (`mvn spring-boot:run`), open
`http://localhost:8080/dashboard.html` in a browser to view live inventory,
per-channel prices, and the audit trail.

## 10. Team Contributions

| Person | Milestone 2 Ownership | Milestone 3 Contribution |
|---|---|---|
| Aditi Bailur | Inventory (Singleton), Observer wiring, `PricingCommand`/`UpdatePriceCommand` | JPA/H2-backed `InventoryRepository` + `InventoryEntity` (persistent storage, replacing CSV); `ScheduledRepricingWorkflow` (new Template Method subclass); DB config; `InventoryRepositoryTest` |
| Yashika Lodh | `PricingStrategy` implementations, `SalesChannelFactory` | New channel implementations (own website, social commerce) via Factory Method; brand-positioning price guardrails (in progress) |
| Pratham Rathod | `AuditLoggingCommandDecorator`, `PricingCommandInvoker`, `RepricingWorkflow` | Spring Boot REST API layer (`PricingEngineApplication`, `InventoryController`, `PricingController`, `AuditController`); Milestone 3 write-up + Design Document |
| Sai Vinayaka Venkata Prateek Kacham | `DemandSignalAdapter`, Shopify integration layer | Real (non-mocked) Shopify GraphQL transport (`HttpGraphQLExecutor`); CSV-backed demand history (`DemandSignalRepository`); basic web dashboard (`dashboard.html`) |

## 11. GitHub Repository
https://github.com/yashika-lodh/CSYE6300_Group_1.git

## 12. Additional Documentation
- Design Document: `docs/DesignDocument_Milestone3.md`
- Milestone 2 Design Document: `docs/DesignDocument_Milestone2.md`
- Updated Milestone 3 write-up: `Milestone3_Document.pdf`