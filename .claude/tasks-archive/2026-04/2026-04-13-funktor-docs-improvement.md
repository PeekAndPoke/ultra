# Funktor Docs Improvement Plan

**Date:** 2026-03-31
**Goal:** Improve 5 thin/stub Funktor doc pages using FunktorConf demo as example source



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `172c4f4f` | 2026-04-03 | 12 | code quality, bug fixes, unit tests, docs cache |
| `2544b7a0` | 2026-04-02 | 1 | code quality, bug fixes, unit tests, docs |
| `f1342275` | 2026-04-02 | 71 | code quality, bug fixes, unit tests, docs |
| `3ebeab04` | 2026-03-31 | 5 | code quality, bug fixes, unit tests |

Archived by `d71db9f8`, `28973c5e` (rename only — that commit's code belongs to another task).

### Files changed (83)

Listed by module — full paths via the command below.

- **docs-site/src** — 49 files
- **kraft/examples** — 15 files
- **funktor/all** — 6 files
- **ultra/cache** — 5 files
- **docs-site/public** — 3 files
- **ultra/semanticui** — 3 files
- **funktor-demo/server** — 2 files

```
git show --stat 172c4f4f 2544b7a0 f1342275 3ebeab04
```

## Current State

- 6 pages GOOD (index, getting-started, core, rest, cluster, testing)
- 4 pages THIN (auth, messaging, insights, staticweb)
- 1 page STUB (logging)

## Approach

Add real code examples derived from the FunktorConf demo (`funktor-demo/server/`).
Keep the existing page structure — extend, don't rewrite.

## Pages to Improve

### 1. logging.astro (STUB -> GOOD)

Currently ~43 lines, no code. Add:

- Logback appender configuration (from `server.kt` KarangoLogAppender)
- Level filtering setup
- Web UI mention with insights integration
- REST API query example

### 2. auth.astro (THIN -> GOOD)

Currently ~97 lines, 1 code example. Add:

- Provider configuration code (Google/GitHub client IDs from `AdminUserRealm.kt`)
- JWT generation example (from `AdminUserRealm.generateJwt()`)
- Kontainer module wiring (from `admin_user_module.kt`)
- Password policy customization snippet

### 3. messaging.astro (THIN -> GOOD)

Currently ~85 lines, 1 code example. Add:

- AWS SES sender configuration (from `kontainer.kt` AwsSesSender setup)
- Dev override with OverridingEmailSender (from `kontainer.kt`)
- Email hooks wiring (from `kontainer.kt` HooksEmailSender)
- SentMessages storage query pattern

### 4. insights.astro (THIN -> GOOD)

Currently ~66 lines, no code. Add:

- Enabling insights in kontainer blueprint
- Mounting InsightsGui (from `server.kt`)
- Per-request data flow explanation with code
- How insights data appears in API responses

### 5. staticweb.astro (THIN -> GOOD)

Currently ~60 lines, 2 code examples. Add:

- Web resource configuration with cache busting
- Virtual host routing pattern (from `server.kt` admin vs API)
- Full template composition with Semantic UI

## Files to Modify

- `docs-site/src/pages/ultra/funktor/logging.astro`
- `docs-site/src/pages/ultra/funktor/auth.astro`
- `docs-site/src/pages/ultra/funktor/messaging.astro`
- `docs-site/src/pages/ultra/funktor/insights.astro`
- `docs-site/src/pages/ultra/funktor/staticweb.astro`
- `docs-site/public/llms.txt` — update Funktor section
- `docs-site/public/llms-full.txt` — regenerate

## Related Work Completed (April 2, 2026)

- **Fomantic UI examples 100% complete** — All 30 component pages (18 Elements, 6 Collections,
  6 Views) fully implemented with live Kotlin code examples. 9 stubs built from scratch (Rail, Step,
  Breadcrumb, Input, Feed, Advertisement, Emoji, Form, Menu). 2 partial pages completed (Button
  66%→100%, Table 50%→100%). Emoji page has searchable catalog of 3,057 shortnames.
- **Funktor API tests** — All modules now have test coverage. 39 test files total (was 28).

Outside this plan's scope, additional docs-site work has been completed:

- **Cache section** — 5 pages: overview (behaviour matrix), getting-started, eviction (4 behaviours), background
  refresh (stale-while-revalidate), observability (onEviction + statistics)
- **Cache behaviours** — 4 new: expireAfterWrite, onEviction, statistics, refreshAfterWrite. Plus MissAction tracking
  and eviction listener infrastructure.
- **Datetime section** — 12 pages: overview, getting-started, type system & guard rails, 6 dedicated type pages (
  MpInstant, MpLocalDate, MpLocalTime, MpLocalDateTime, MpZonedDateTime, MpTimezone), formatting & parsing, ranges &
  time slots, Kronos clock
- **Cache section** — 3 pages: overview, getting-started, eviction policies
- **Kontainer** — added introspection/debugging page, lazy `by` delegation example
- **Streams** — added foldNotNull, historyOfNonNull, filterIsInstance, permanent() examples
- **LLMs** — created datetime.md, cache.md; updated llms.txt index
- **Navigation** — "Tools" top-nav dispatcher page, category-tinted Hilbert backgrounds on all landing pages, subtle
  body tints on detail pages
- **Landing pages** — Kraft and Funktor index pages converted to standalone card-based dispatchers (matching Tools
  style)

## Verification

- `cd docs-site && pnpm run build` — zero errors, correct page count
