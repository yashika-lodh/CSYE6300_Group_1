# Final Submission — Action Plan
## Dynamic Pricing Engine for Multi-Channel DTC Retail — Group 1

This document tracks what's left to finish the project after Milestone 3, in the order the team should tackle it.

---

## 1. Finish the In-Progress Branches

These are the items carried over from the Milestone 3 doc's "Planned for Final Submission" section.

### `channels-integration` — Owner: Yashika Lodh
- [ ] `OwnWebsiteChannel` implementation
- [ ] `SocialCommerceChannel` implementation
- [ ] Wire both new channel types into `SalesChannelFactory`
- [ ] Add min/max price guardrail fields to `PricingContext`
- [ ] Enforce guardrails in all 3 `PricingStrategy` implementations

### `shopify-integration-dashboard` — Owner: Sai Vinayaka Venkata Prateek Kacham
- [ ] Replace `MockGraphQLExecutor` with a real HTTP-based `GraphQLExecutor` (requires a Shopify dev store + API credentials)
- [ ] Basic web dashboard: current prices, demand trends, audit history

---

## 2. Merge Everything into `main`

```bash
git checkout main
git merge channels-integration
git merge shopify-integration-dashboard
```

Likely conflict points to watch for:
- `SalesChannelFactory` — new `ChannelType` enum cases
- `PricingContext` / pricing strategy classes — new guardrail fields
- `pom.xml` — new dependency for the real HTTP/GraphQL client

---

## 3. Run the Full Test Suite After Merging

```bash
mvn test
```

- [ ] Add tests for the new channels if not already covered
- [ ] Add tests confirming guardrails clamp out-of-bound prices correctly

---

## 4. End-to-End Sanity Check

```bash
mvn spring-boot:run
```

- [ ] Hit `/api/inventory` — confirm stock reads/writes work
- [ ] Hit `/api/pricing/{sku}/reprice` — confirm new channels appear in the response
- [ ] Hit `/api/audit` — confirm the compliance log includes the new channels' commands
- [ ] Confirm a price outside the guardrail bounds actually gets clamped

---

## 5. Write the Final Submission Deliverables

Same three-part structure as Milestone 2 / Milestone 3:

- [ ] **Updated Final Document** — mark all items "Done," update Tech Stack if a new HTTP/GraphQL library was added, final Team Contributions table
- [ ] **Final Design Document** — update "Planned for Final Submission" section to reflect completion; update System Architecture to include the new channels and real Shopify integration

---

## 6. Repo Hygiene Before Final Submission

- [ ] Confirm TA `vaibhavsingh97` is still listed as a collaborator (Settings → Collaborators)
- [ ] Delete or archive `channels-integration` and `shopify-integration-dashboard` branches after merging
- [ ] Update root `README.md` to reflect final status, not Milestone 2/3 language
- [ ] Confirm no real Shopify API keys/secrets are committed — use environment variables or a `.env` file listed in `.gitignore`

---

## 7. Prep for Demo / Presentation

- [ ] Each team member should be ready to explain their own files, especially the most recently added ones (channels, guardrails, real Shopify integration, dashboard)
- [ ] Do a full dry run of the demo (console `Main` + REST API + dashboard) before presenting
