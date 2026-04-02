# Funktor Docs Improvement Plan

**Date:** 2026-03-31
**Goal:** Improve 5 thin/stub Funktor doc pages using FunktorConf demo as example source

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

## Related Work Completed (April 2026)

Outside this plan's scope, additional docs-site work has been completed:

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
