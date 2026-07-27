# CSYE 6300 - Final Project Milestone 2

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
| Singleton | Creational | Single shared `Inventory` instance across all channels |
| Strategy | Behavioral | Interchangeable pricing algorithms (cost-plus, competitor-aware, inventory-aging) |
| Factory Method | Creational | Creating different channel implementations (Shopify, etc.) via `SalesChannelFactory` |
| Command | Behavioral | Encapsulating price-change requests with undo/redo support (`PricingCommand`, `PricingCommandInvoker`) |
| Decorator | Structural | Adding audit logging to pricing commands dynamically (`AuditLoggingCommandDecorator`) |
| Observer | Behavioral | Triggering repricing when inventory levels change (`InventoryObserver`, `RepricingTriggerObserver`) |
| Adapter | Structural | Adapting demand signal data to the domain model (`DemandSignalAdapter`) |
| Template Method | Behavioral | Defining the repricing workflow skeleton: fetch → validate → execute → log (`RepricingWorkflow`) |

## 6. Tech Stack
- **Language**: Java (JDK 21)
- **Build Tool**: Maven 3.8+
- **IDE**: Eclipse or VS Code with Java extensions
- **Version Control**: Git + GitHub (private repo)
- **Testing**: JUnit 5
- **Data Source**: CSV files for inventory and demand signals (`data/inventory.csv`, `data/demand_signals.csv`)
- **Logging**: SLF4J + Logback
- **External Integration**: Shopify Admin GraphQL API via `ShopifyApiClient`, backed by `MockGraphQLExecutor` for credential-free testing
- **Planned (Milestone 3)**: Spring Boot for REST API

## 7. Functionalities Completed in Milestone 2

### Core Pattern Implementation
- [x] Inventory (Singleton) with thread-safe stock tracking and observer notification
- [x] SalesChannel interface with Shopify implementation
- [x] PricingStrategy interface with 3 concrete strategies:
  - CostPlusMarkupStrategy
  - CompetitorAwarePricingStrategy
  - InventoryAgingStrategy
- [x] PricingCommand with execute/undo support and full history
- [x] AuditLoggingCommandDecorator wrapping all price updates with compliance logging
- [x] Observer pattern wired so inventory changes trigger repricing checks

### Lightweight AI + Decision Logic
- [x] DemandForecaster using simple moving average (7-day vs 30-day)
  - Detects trend: RISING (>10% growth), FALLING (<-10% decay), STABLE
  - No external ML library — pure statistical logic
- [x] PricingStrategySelector that dynamically picks strategy based on demand trend
  - RISING demand → CompetitorAwarePricingStrategy (aggressive positioning)
  - STABLE demand → CostPlusMarkupStrategy (margin optimization)
  - FALLING demand → InventoryAgingStrategy (clear old stock)

### Shopify API Integration
- [x] ShopifyApiClient with GraphQL support
  - Fetch live product data and inventory
  - Update prices in real-time via mutation
  - Mock-based tests (no real credentials needed) via `MockGraphQLExecutor`
- [x] ShopifyChannel implementing SalesChannel interface
  - Caching with 5-minute TTL
  - Error handling with fallback to cached prices

### Testing
- [x] JUnit 5 suite covering Inventory, all 3 pricing strategies, Command undo/redo, the audit Decorator, and DemandForecaster trend detection (20 tests total)

## 8. Functionalities Planned for Milestone 3
- [ ] Spring Boot REST API exposing repricing, inventory, and audit-report endpoints
- [ ] Additional SalesChannel implementations: own website and social commerce
- [ ] Persistent storage (replacing CSV files) for inventory, demand history, and audit logs
- [ ] Scheduled/automated repricing runs, not just Observer-triggered
- [ ] Basic web dashboard for prices, demand trends, and audit history
- [ ] Real (non-mocked) Shopify GraphQL transport behind a feature flag
- [ ] Brand-positioning guardrails (min/max price bounds per SKU)

## 9. Build & Run

```bash
mvn compile
mvn test
mvn compile exec:java -Dexec.mainClass="com.csye6300.group1.pricingengine.Main"
```

## 10. Contributions

| Person | Module | Responsibility |
|---|---|---|
| Aditi Bailur | Singleton, Observer & Command Core | `Inventory` (Singleton), Observer wiring (`InventoryObserver`, `StockChangeEvent`, `RepricingTriggerObserver`), `PricingCommand`/`PriceReceiver`/`UpdatePriceCommand` |
| Yashika Lodh | Strategy & Factory Method | `PricingStrategy` + 3 concrete strategies, `SalesChannelFactory`, `SalesChannel` interface, demand domain models |
| Pratham Rathod | Decorator, Invoker & Template Method | `AuditLoggingCommandDecorator`, `PricingCommandInvoker`, `RepricingWorkflow`/`StandardRepricingWorkflow`, `PricingStrategySelector` |
| Sai Vinayaka Venkata Prateek Kacham | Adapter & Shopify Integration | `DemandSignalAdapter`, `ShopifyApiClient`, `MockGraphQLExecutor`, `ShopifyChannel`, `ChannelManager` |

## 11. GitHub Repository
https://github.com/yashika-lodh/CSYE6300_Group_1.git

## 12. Additional Documentation
- Design Document: `docs/DesignDocument_Milestone2.md`
- Updated Milestone 2 write-up: `Milestone2_Document.docx`