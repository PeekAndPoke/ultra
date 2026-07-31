# Query parameter VALUES are recorded into insights, with no way to opt out

**Status:** FOUND, NOT FIXED — needs a design decision
**Security-critical:** yes — data exposure, not injection
**Found:** 2026-07-31, while removing Jackson from the query printers

## What happens

`KarangoDriver.kt:109` sets `profilerEntry.vars = vars` — the **bind variable values** of every query —
and `VaultCollector` puts the whole `QueryProfiler.Entry` into the insights record. The old GUI rendered
them (`reference/collectors/VaultCollector.kt`, `json(it.vars)`), and the Vue tab will too unless
something changes (`funktor/insights/reference/TAB-SPECS.md`, vault section).

So every value that has ever been bound into a query is written to disk and served to a superuser:

- a user search term — other people's names, emails, whatever was typed
- an auth lookup — the token, session id or activation code **being looked up**
- anything else a repository is asked to find something by

**This is not injection.** Verified while investigating: `query.query` keeps its `@placeholders` and
`vars` travel separately to the driver (`KarangoDriver.kt:124-127`), so values never enter the query
text. `AqlPrinter.raw`, which does substitute them, has **zero production call sites** — every consumer
is under `src/test/`. The exposure is the recording, not the execution.

## Why it needs a decision rather than a fix

The obvious answers are all wrong on their own:

- **Drop `vars` entirely** — kills most of the profiler's debugging value. Knowing a query was slow
  without knowing what it was slow *for* is close to useless.
- **Redact by parameter name** — the names are generated (`@v1`, `@v2`) or derived from field names, so
  there is nothing meaningful to match on. This is exactly where the `HeaderLogging` approach fails.
- **`Redacted<T>` on the values** — they are arbitrary bound values, not declared fields. There is no
  declaration site.

The maintainer's framing (2026-07-31): **a way to override this per query or per repository.** Neither
shape is obvious.

Sketches, none decided:

| Shape | Note |
|---|---|
| Per-repository default | `KarangoRepository` declares whether its query vars may be recorded. Coarse, but repositories are exactly where "this one holds credentials" is known — the auth/session repos would opt out wholesale |
| Per-query opt-out | An option on the query builder, e.g. `.noProfilingVars()`. Precise, but per-route `.noInsights()` has already shown that per-call-site controls get forgotten |
| Both, repo as the floor | Mirrors `ApiRoutes.authFloor`: the repository sets the floor, a query can only tighten it. The house pattern for exactly this problem |
| Record only the SHAPE | Types and arity instead of values — `@v1: String(len=24)`. Keeps most debugging value with no content. Cheapest to make safe by default |

The last one deserves weighing seriously: it needs no per-site decision at all, which is the failure
mode the other three share.

## Related

- `.claude/tasks-archive/2026-07/20260730-insights-rest-api.md` — the API that made this remotely readable
- `.claude/tasks/20260731-config-secrets-in-insights.md` — the config half of the same problem
- `funktor/insights/reference/TAB-SPECS.md` — the vault tab that will render it
