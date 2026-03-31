# Funktor v1.0 Roadmap — Feature Inventory, Maturity & TODO Analysis

**Date:** 2026-03-25 (updated 2026-03-31)
**Scope:** Funktor framework, Monko (MongoDB), database portability

---

## Executive Summary

Funktor is a full-stack Kotlin framework on Ktor with 10 submodules (~438 source files, ~20 tests).
The critical blocker for v1.0 is **database portability**: currently hardcoded to ArangoDB (Karango).
v1.0 requires all three database backends (ArangoDB, MongoDB, SQL/Exposed) to be pluggable and
feature-complete across all modules that use persistence.

**Monko (MongoDB) is ~20-30% complete vs Karango.** No Exposed (SQL) module exists yet.

---

## Module Inventory

### Architecture

```
┌─────────────────────────────────────────────┐
│  Applications (funktor-demo)                │
├─────────────────────────────────────────────┤
│  funktor/all (aggregation module)           │
├─────────────────────────────────────────────┤
│  High-level:                                │
│  • auth        (authentication/SSO)         │
│  • cluster     (distributed coordination)   │
│  • insights    (metrics/profiling)          │
│  • logging     (structured logs)            │
│  • messaging   (email/notifications)        │
├─────────────────────────────────────────────┤
│  Core:                                      │
│  • core        (app lifecycle, config, CLI) │
│  • rest        (API routing & codec)        │
├─────────────────────────────────────────────┤
│  Infrastructure:                            │
│  • testing     (test utilities)             │
│  • staticweb   (HTML/CSS/assets)            │
├─────────────────────────────────────────────┤
│  Database:                                  │
│  • Karango     (ArangoDB) ← mature          │
│  • Monko       (MongoDB)  ← incomplete      │
│  • Exposed     (SQL)      ← does not exist  │
│  • ultra/vault (abstraction layer)          │
└─────────────────────────────────────────────┘
```

### Module Statistics

| Module    | Src Files | Test Files | Test Ratio | KDoc | TODOs  | DB-Dependent                |
|-----------|-----------|------------|------------|------|--------|-----------------------------|
| core      | 57        | 5          | 9%         | 61%  | 0      | No (uses Vault abstraction) |
| rest      | 52        | 6          | 12%        | ~50% | 6      | No                          |
| auth      | 44        | 9          | 20%        | 65%  | 5      | **Yes** (Karango + Monko)   |
| cluster   | 76        | 7          | 9%         | 55%  | 2      | **Yes** (Karango only)      |
| insights  | 30        | 0          | 0%         | ~40% | 0      | No                          |
| logging   | 15        | 0          | 0%         | ~40% | 1      | **Yes** (Karango only)      |
| messaging | 25        | 0          | 0%         | ~40% | 0      | **Yes** (Karango only)      |
| staticweb | 22        | 1          | 5%         | ~30% | 0      | No                          |
| testing   | 6         | 0          | 0%         | ~20% | 0      | No                          |
| all       | 1         | 0          | -          | -    | 0      | No                          |
| **TOTAL** | **328**   | **28**     | **9%**     | ~50% | **14** | 4 modules                   |

### Database-Dependent Modules

These modules contain ArangoDB-specific repository code that must be ported:

| Module        | Karango Repos                         | Monko Repos          | Exposed Repos | Port Complexity                      |
|---------------|---------------------------------------|----------------------|---------------|--------------------------------------|
| **auth**      | KarangoAuthRecordsRepo                | MonkoAuthRecordsRepo | -             | Low (2 queries)                      |
| **cluster**   | 4 repos (jobs, locks, storage, cache) | -                    | -             | High (complex queries, TTL, upsert)  |
| **logging**   | KarangoLogRepository + Storage        | -                    | -             | Medium (filtering, TTL, bulk update) |
| **messaging** | KarangoSentMessagesRepo               | -                    | -             | Medium (array expansion, paging)     |

---

## Per-Module Maturity Analysis

### core — Maturity: GOOD

**What it does:** App lifecycle, config, CLI, broker/routing, fixtures, repair system, session management.

**Strengths:**

- Clean architecture with no database coupling
- Type-safe routing (TypedRoute sealed hierarchy)
- Comprehensive parameter conversion (primitives, Java Time, mpDateTime, Vault entities)
- CLI infrastructure with extensible commands
- Fixture loading and data repair systems

**Gaps:**

- 9% test ratio — broker parameter conversion is tested, but lifecycle, CLI, and fixtures are not
- WebSocket support exists but unclear how mature

**v1.0 TODOs:**

- [ ] Add tests for CLI commands, lifecycle hooks, fixture loading
- [ ] Document the repair system
- [ ] Verify WebSocket support completeness

---

### rest — Maturity: GOOD

**What it does:** REST API framework, typed routing, authorization, codec, Dart code generation, Postman docs.

**Strengths:**

- Type-safe API routes (ApiRoute sealed hierarchy with params/body/response types)
- Authorization DSL (role, permission, org, group checks with AND/OR combinators)
- Slumber-based REST codec with caching
- Dart code generation for mobile clients (28 files)
- Postman collection export
- Attack detection (WordPress, .php probes)
- Insights enrichment on every response

**Gaps:**

- 12% test ratio — Dart codegen has 6 TODOs in tests
- Dart codegen is substantial but undertested

**v1.0 TODOs:**

- [ ] Complete Dart codegen test coverage (6 TODOs)
- [ ] Document the authorization DSL
- [ ] Document Dart code generation setup

---

### auth — Maturity: FAIR

**What it does:** Multi-realm authentication with email/password, Google SSO, GitHub SSO. Storage adapters for Karango
and Monko.

**Strengths:**

- Clean adapter pattern (AuthRecordStorage with Null/Vault/Karango/Monko implementations)
- Already has Monko adapter — auth is the only module with MongoDB support today
- Multiple auth providers with capability-based feature detection
- Password policy enforcement
- JWT integration
- Good test coverage relative to other modules (9 tests)

**Gaps:**

- Account activation not implemented (`// TODO: implement me`)
- Activation email not sent (`// TODO: send account activation email`)
- Token length and expiration hardcoded (3 TODOs)
- Frontend URL building hardcoded

**v1.0 TODOs:**

- [ ] Implement account activation flow
- [ ] Make token length and expiration configurable
- [ ] Build frontend URLs from app config
- [ ] Add Exposed (SQL) adapter
- [ ] Add integration tests for Google/GitHub SSO flows

---

### cluster — Maturity: FAIR (largest module, most DB-dependent)

**What it does:** Distributed coordination — background jobs, file depot (FS + S3), distributed locks, server beacons,
random data/cache storage, worker framework.

**Subcomponents:**

| Component      | Files | Karango Repos            | Complexity                        | Port Effort |
|----------------|-------|--------------------------|-----------------------------------|-------------|
| backgroundjobs | ~20   | Queue + Archive repos    | High (claim-next-due, retry, TTL) | High        |
| depot          | ~15   | None (FS + S3)           | Low                               | None needed |
| locks          | ~20   | Lock + Beacon repos      | High (distributed coordination)   | High        |
| storage        | ~15   | RandomData + Cache repos | Medium (upsert, UNSET)            | Medium      |
| workers        | ~15   | Worker state via Vault   | Medium                            | Medium      |

**Strengths:**

- Comprehensive distributed systems toolkit
- Depot abstraction (FS + S3) is database-independent
- Background job system with retry policies and archival

**Gaps:**

- Only Karango implementations — no Monko, no Exposed
- Depot has no encryption (`// TODO: apply encryption`)
- 9% test ratio on a complex distributed system

**v1.0 TODOs:**

- [ ] Add Monko implementations for all 4 Karango repos
- [ ] Add Exposed implementations for all 4 repos
- [ ] Add encryption for depot file storage
- [ ] Add tests for background job queue/claim/retry logic
- [ ] Add tests for distributed lock coordination
- [ ] Test worker framework lifecycle

---

### logging — Maturity: POOR

**What it does:** Structured logging with ArangoDB persistence, severity filtering, state management, bulk actions.

**Gaps:**

- Zero tests
- Only Karango implementation
- Fulltext search uses substring matching (`// TODO: use fulltext index`)
- No Monko or Exposed adapter

**v1.0 TODOs:**

- [ ] Add Monko adapter
- [ ] Add Exposed adapter
- [ ] Implement fulltext index for log search
- [ ] Add tests for log filtering, bulk actions, TTL expiry

---

### messaging — Maturity: POOR

**What it does:** Email system with SMTP, AWS SES, SendGrid backends. Sent message storage via Karango.

**Gaps:**

- Zero tests
- Only Karango storage
- No Monko or Exposed adapter for sent messages

**v1.0 TODOs:**

- [ ] Add Monko adapter for SentMessagesStorage
- [ ] Add Exposed adapter for SentMessagesStorage
- [ ] Add tests for email sending (mock transports)
- [ ] Add tests for sent message storage and retrieval

---

### insights — Maturity: POOR

**What it does:** Performance monitoring, CPU profiling, request metrics dashboard.

**Gaps:**

- Zero tests
- JVM-only (no multiplatform)
- No documentation

**v1.0 TODOs:**

- [ ] Add basic tests for metrics collection
- [ ] Document the insights dashboard setup

---

### staticweb — Maturity: FAIR

**What it does:** Static web assets, templates (Semantic UI), flash sessions, PrismJS code highlighting.

**Gaps:**

- 5% test ratio
- Template system is tightly coupled to Semantic UI

**v1.0 TODOs:**

- [ ] Add tests for template rendering
- [ ] Document template system

---

### testing — Maturity: ADEQUATE

**What it does:** Test utilities, Kotest integration, test harnesses for Funktor apps.

Small utility module — adequate for its purpose.

---

## Monko (MongoDB) — Gap Analysis vs Karango

### Current State: ~20-30% complete

| Feature                     | Karango                               | Monko                 | Status                           |
|-----------------------------|---------------------------------------|-----------------------|----------------------------------|
| Driver (connect, basic ops) | Complete                              | Complete              | Parity                           |
| Repository base class       | Complete                              | Partial               | `save()` and `remove()` are TODO |
| Insert                      | Complete                              | Complete              | Parity                           |
| Find by ID                  | Complete                              | Complete              | Parity                           |
| Find all                    | Complete                              | Complete              | Parity                           |
| Save (update)               | Complete                              | **TODO**              | Blocker                          |
| Remove                      | Complete                              | **TODO**              | Blocker                          |
| Batch insert                | Complete                              | Missing               | Needed                           |
| Index management            | Complete (validate, recreate, ensure) | Basic (recreate only) | Gap                              |
| Query DSL                   | 46 files, 100+ functions              | **None**              | Critical blocker                 |
| KSP code generation         | Complete + 3 test files               | Basic + 0 tests       | Gap                              |
| Type-safe property paths    | Complete                              | Basic                 | Gap                              |
| Cursor (fullCount, timeMs)  | Complete                              | **TODO**              | Gap                              |
| Repository hooks            | Complete (before/after save, delete)  | Partial               | Gap                              |
| Exception handling          | Complete                              | None                  | Gap                              |
| Soft delete addon           | Complete                              | None                  | Nice-to-have                     |
| Test coverage               | 148 test files                        | **0 test files**      | Critical blocker                 |

### Monko Priority Actions

**P0 — Blockers:**

1. Implement `save()` (update) in MonkoRepository
2. Implement `remove()` in MonkoRepository
3. Build MongoDB query DSL (equivalent to Karango's AQL DSL)
4. Add test suite (start with repository CRUD, then query DSL)

**P1 — Required for Funktor v1.0:**

5. Add batch insert support
6. Complete cursor features (fullCount, timeMs)
7. Complete KSP code generation + tests
8. Add comprehensive index management (validate, ensure)
9. Add repository hooks (OnAfterDeleteHook)
10. Add exception handling

**P2 — Nice to have:**

11. Soft delete addon
12. Update README (currently copy-pasted from Karango)

---

## Exposed (SQL) — Does Not Exist

An entirely new module needs to be created. The approach should mirror Karango/Monko:

1. **exposed/core** — Driver, repository base class, query DSL
2. **exposed/ksp** — Code generation for type-safe property paths

### Key Challenges for SQL:

- Polymorphic types (`_type` discriminator) → Single Table Inheritance or Joined Table strategy
- Document-style nested objects → JSON columns or normalized tables
- TTL indexes → Scheduled cleanup jobs or DB-specific features
- Array fields (e.g., `lookup.refs` in messaging) → Junction tables or JSON arrays
- ArangoDB's `UPSERT` → SQL `INSERT ON CONFLICT UPDATE` / `MERGE`

---

## Database Portability — Files That Need Porting

### Auth (already partially done)

| File            | Karango                | Monko                | Exposed  |
|-----------------|------------------------|----------------------|----------|
| AuthRecordsRepo | KarangoAuthRecordsRepo | MonkoAuthRecordsRepo | **TODO** |

### Cluster (4 repositories)

| Repository                | Purpose                       | Karango | Monko    | Exposed  |
|---------------------------|-------------------------------|---------|----------|----------|
| BackgroundJobsQueueRepo   | Job queue with claim-next-due | Yes     | **TODO** | **TODO** |
| BackgroundJobsArchiveRepo | Completed job archive         | Yes     | **TODO** | **TODO** |
| Lock/Beacon repos         | Distributed locking           | Yes     | **TODO** | **TODO** |
| RandomDataRepository      | Key-value storage             | Yes     | **TODO** | **TODO** |
| RandomCacheRepository     | Cached data with TTL          | Yes     | **TODO** | **TODO** |

### Logging (1 repository)

| Repository              | Purpose                              | Karango | Monko    | Exposed  |
|-------------------------|--------------------------------------|---------|----------|----------|
| LogRepository + Storage | Log persistence, filtering, bulk ops | Yes     | **TODO** | **TODO** |

### Messaging (1 repository)

| Repository       | Purpose                         | Karango | Monko    | Exposed  |
|------------------|---------------------------------|---------|----------|----------|
| SentMessagesRepo | Email archive, ref-based lookup | Yes     | **TODO** | **TODO** |

**Total porting work: 7 repositories x 2 backends = 14 new repository implementations**

---

## v1.0 Phased Roadmap

### Phase 0 — Foundation (Monko basics)

Complete Monko's missing fundamentals:

- [ ] Implement `MonkoRepository.save()` and `remove()`
- [ ] Add cursor fullCount and timeMs
- [ ] Add basic test suite for CRUD operations
- [ ] Update Monko README

### Phase 1 — Monko Query DSL

Build MongoDB query DSL equivalent to Karango's AQL DSL:

- [ ] Design DSL API (filter, sort, limit, project, aggregate)
- [ ] Implement type-safe filter expressions using KSP-generated property paths
- [ ] Implement aggregation pipeline builder
- [ ] Add sort, limit, skip support
- [ ] Add test suite for query DSL

### Phase 2 — Monko KSP + Repository Features

- [ ] Complete KSP code generation (test all paths)
- [ ] Add batch insert support
- [ ] Add comprehensive index management
- [ ] Add repository hooks (full lifecycle)
- [ ] Add exception handling

### Phase 3 — Port Funktor Modules to Monko

- [ ] Port auth (already done — verify and test)
- [ ] Port logging (KarangoLogRepository → MonkoLogRepository)
- [ ] Port messaging (KarangoSentMessagesRepo → MonkoSentMessagesRepo)
- [ ] Port cluster (4 repositories — hardest phase)
- [ ] Add integration tests for each ported module

### Phase 4 — Exposed (SQL) Module

- [ ] Create exposed/core with driver and repository base class
- [ ] Design SQL schema strategy for polymorphic types
- [ ] Implement KSP code generation for SQL property paths
- [ ] Implement query DSL on top of Exposed
- [ ] Port all 7 repositories to Exposed

### Phase 5 — Funktor Polish

- [ ] Implement account activation in auth
- [ ] Make auth token config configurable
- [ ] Add depot encryption
- [ ] Complete Dart codegen tests
- [ ] Add tests for all zero-coverage modules (insights, logging, messaging)
- [ ] Documentation for all modules

### Phase 6 — Demo App

- [ ] Update funktor-demo to demonstrate DB switching (Karango ↔ Monko ↔ Exposed)
- [ ] Add integration test suite using testing module
- [ ] Verify all features work across all three backends

---

## Risk Assessment

| Risk                                                               | Impact | Likelihood | Mitigation                                                     |
|--------------------------------------------------------------------|--------|------------|----------------------------------------------------------------|
| MongoDB query DSL design doesn't map cleanly from AQL              | High   | Medium     | MongoDB aggregation pipeline is flexible; design DSL around it |
| SQL polymorphic types create schema complexity                     | High   | High       | Start with JSON columns for nested objects; normalize later    |
| Cluster distributed locking semantics differ across DBs            | High   | Medium     | Define abstract locking contract; test thoroughly              |
| KSP maintenance burden with 3 code generators                      | Medium | High       | Share base infrastructure between Karango/Monko/Exposed KSP    |
| TTL index behavior varies (ArangoDB native vs MongoDB vs SQL cron) | Medium | Medium     | Abstract TTL behind repository interface                       |

---

## Estimated Effort

| Phase   | Scope                 | Rough Effort            |
|---------|-----------------------|-------------------------|
| Phase 0 | Monko basics          | Small                   |
| Phase 1 | Monko query DSL       | Large (core feature)    |
| Phase 2 | Monko KSP + features  | Medium                  |
| Phase 3 | Port 7 repos to Monko | Medium                  |
| Phase 4 | Entire Exposed module | Very Large (new module) |
| Phase 5 | Funktor polish        | Medium                  |
| Phase 6 | Demo app              | Small                   |
