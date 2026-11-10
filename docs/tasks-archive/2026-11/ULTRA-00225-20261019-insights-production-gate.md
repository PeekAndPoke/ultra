# ULTRA-00225: Insights: a production gate

> **Status (2026-11-09): DONE.** All four items decided by the maintainer 2026-10-19 (relayed by Mission-Control). Built 2026-11-09 (maintainer: "I would like to see the insights getting finished"). Gate PASS in round 1 (two charters, both CLEAN); the MINOR batch applied and mutation-checked. Two maintainer questions below.
>
> **Part of:** ULTRA-00253 (Insights epic)

**Plan:** ULTRA-00221 (insights auth parallel plan) → the "production gate for insights" row
**Security-critical:** yes — red-team: ULTRA-00161 (redteam insights api) (scenarios 11, 24-31)

## Decided

- [x] **The graph endpoints answer 404 while insights is disabled** (maintainer 2026-10-19). `GET /_/funktor/insights/
      graphs/kontainer` and `/graphs/database` are served today whatever `InsightsConfig.enabled` says, exposing ~180
      service definitions with code locations and the repository graph to a superuser on an app that records nothing.
      E2e on both backends; mutation-checked.
- [x] **The record endpoints answer 404 while insights is disabled too** (maintainer 2026-10-19), consistent with the
      graphs. E2e on both backends; mutation-checked.
- [x] **Refuse `enabled = true` in production** (maintainer 2026-10-19). Today nothing does: no environment
      allow-list, no startup refusal (residue of the archived ULTRA-00057 (insights gui auth gate); red-team
      scenario 11 presumes one). Decide the mechanism with the existing environment/`AppInfo` signal; startup must fail
      loudly, not silently disable. E2e: a prod-configured app with insights enabled does not boot.
- [x] **Redact JVM system properties in `RuntimeCollector`** (`RuntimeCollector.kt:58`, every request, anonymous ones
      too) — maintainer: "redact if easily possible". Easy = reuse the existing redaction (`Redacted`/key-name rules
      the app-config slice uses). If that turns out non-trivial, STOP and raise it with what it would take; dropping
      the properties is the fallback.

## Implementation notes

- **The mechanism: an allow-list** (`funktor/insights/src/jvmMain/kotlin/InsightsEnvironment.kt`). Insights may run
  only where `KtorConfig.isDevelopment` holds (`dev`, `test`, `qa*`). Every other name counts as production: `prod` and
  `live`, but also `staging`, a typo, or an unresolved `${ENV}`. This is the direction ULTRA-00052 (env classification
  allowlist) asks of the whole classification. `isProduction` alone would have let `staging` through.
- **Three layers, one predicate** (`InsightsConfig?.isActiveIn(ktor)`):
  1. the boot: `Route.instrumentWithInsights(config, ktor)` now takes the app's `KtorConfig` (breaking) and throws
     `AppStartException` when an enabled config meets a non-development environment. A parameter rather than reading the
     `App` off the application attributes, because `funktor/testing` puts it there too late (`setup.kt:52`, "currently
     called too late");
  2. the kontainer: `dynamic(Insights)` hands out `InsightsDisabled` unless the config is active in `AppConfig.ktor`, so
     an app that injects an enabled config without instrumenting records nothing either;
  3. the API: every route calls `requireActiveInsights()` (`api/InsightsApi.kt`) and throws `NotFoundException` unless
     the request kontainer's `InsightsConfig` is active in the app's environment. It runs after the floor, so a caller
     below superuser keeps getting the floor's 401 and the floor's zero-work-before-auth rule holds.
- **Redaction** (`collectors/RuntimeCollector.kt`, `RuntimeCollector.redact`): reuses the header rule's name pattern
  (`HeaderLogging.sensitiveByName`, made `internal`), plus `key|pass|pwd`, which property names use more than headers do,
  and always `sun.java.command` (the main class and its arguments). Values become `HeaderLogging.REDACTED`.
- "E2e on both backends": the 404s do not touch a database. The funktor-all app has both backends wired, and the
  database graph it 404s lists repositories of both (`InsightsDatabaseGraphBackendsSpec`), so one run covers it.

## Test evidence

- `funktor/insights/src/jvmTest/kotlin/InsightsEnvironmentSpec.kt` (NEW): the allow-list over 15 names; active =
  enabled AND allowed; the refusal names the environment and never fires for a disabled config; **a ktor app whose
  routing instruments with insights enabled does not start in `prod`, and does in `dev`**; the kontainer hands out
  `InsightsDisabled` in `prod`, `InsightsFull` in `dev`; redaction of 7 secret-looking names, 2 kept.
- `collectors/CollectorRedactionSpec.kt`: `RuntimeCollector.finish` on a real call redacts a secret-looking property.
- `funktor/all/src/jvmTest/kotlin/InsightsProductionGateSpec.kt` (NEW): a record is written on the `insights.*` host;
  on the `api.*` host (same blueprint, same depot, insights off) a superuser gets 404 from all four endpoints, **for
  that existing record too**, while the insights host answers 200 to the same four calls (the control). Anonymous gets
  401 from all four on the off host.
- Existing specs whose superuser calls ran on the `api.*` host moved to the `insights.*` host: `InsightsApiSpec`
  (3 cases), `InsightsGraphsApiSpec` (2 call sites), `InsightsKontainerSliceSpec` (1), `InsightsDatabaseGraphBackendsSpec` (1).
- Runs: `:funktor:insights:jvmTest` 101/0, `:funktor:all:jvmTest` 268/0.

### Mutation table (2026-11-09, one mutant at a time, restore `cmp`-verified)

| # | Mutant | Red in |
|---|---|---|
| G1 | the API gate never throws | `InsightsProductionGateSpec` "a superuser gets 404 from every endpoint" |
| G2–G5 | `requireActiveInsights()` removed from one endpoint each (list, one record, kontainer graph, database graph) | the same case, each alone |
| G6 | `allowsInsights = !isProduction` (deny-list) | `InsightsEnvironmentSpec` allow-list case and refusal case |
| G7 | no boot refusal in `instrumentWithInsights` | `InsightsEnvironmentSpec` "does not boot in production" |
| G8 | the kontainer checks only `config.enabled` | `InsightsEnvironmentSpec` kontainer case |
| G9 | `finish` records properties unredacted | `CollectorRedactionSpec` RuntimeCollector case |
| G10 | only the header pattern, no `key|pass|pwd` | `InsightsEnvironmentSpec` redaction case |

## Review record — the LEDGER

Round 1, level 1 (`reviewer-high`), two charters by risk: implementation & code style, security. Both **CLEAN**
(0 CRITICAL, 0 MAJOR). MINORs applied as one batch (not re-reviewed, per the loop); the batch mutation-checked.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (both) | `api/InsightsApi.kt:141` | The API's environment term is untested: every API test runs in env `test`, so `config.enabled` alone would stay green | fix | Verified: `AppConfig` is a blueprint instance (`core_module.kt:122`), so no test mount can be `prod`. Fixed by deciding once: `Insights.isActive` (false by default, true in `InsightsFull`); the API asks it, the kontainer decides it, and the `prod` kontainer test covers it. Mutants H1–H3 red |
| 1 | MINOR (impl) | `InsightsEnvironmentSpec.kt:58` | The boot test is bare ktor, not funktor's start path | fix (note) | Verified by reading: the throw is synchronous in the module body (`app.kt:141`) and nothing up to `EngineMain.start` catches it; `routing {}` runs at install time. The bare-ktor test exercises exactly that body. Side fact: `OnAppStarting` hooks run before the refusal |
| 1 | MINOR (impl) | `InsightsEnvironment.kt:14,17,23` | Three public helpers used only in the module | fix | Now `internal` |
| 1 | NIT (impl) | `InsightsEnvironment.kt:29` | The message names `insights.enabled`, an app-specific key | fix | Reworded to the `InsightsConfig` passed to `instrumentWithInsights` |
| 1 | MINOR (sec) | `RuntimeCollector.kt:23-37` | Name-only redaction misses credentials inside values (`-Dmongodb.connectionString=mongodb://root:pw@db`) | fix | Values with URL userinfo carrying a password, or a `password=`/`passwd=`/`pwd=` parameter, are redacted too. Mutant H4 red. A DSN with a key but no password (`https://<key>@host`) is not caught: red-team scenario 40 of ULTRA-00161 |
| 1 | MINOR (sec) | `InsightsDataLoader.kt:48-51` | Records written before keep unredacted properties | fix (docs) | CHANGELOG tells operators to clear the depot on upgrade; production is unaffected (404). Same release-note family as ULTRA-00228 item 2 |
| 1 | MINOR (sec) | `KtorConfig.kt:37` | `qa*` is a prefix: `qatar`, `qa-prod-mirror` run insights | maintainer-decision | Asked 2026-11-09. Tightening `isQa` itself belongs to ULTRA-00052 |
| 1 | NIT (sec) | `insights.astro:20`, `CHANGELOG.md` | "refuses to boot" overstates: only where the config reaches `instrumentWithInsights` | fix | Both reworded; the CHANGELOG says a config that reaches only the kontainer records nothing |
| 1 | note (impl) | `InsightsApi.kt` | The 404 is per mount: an uninstrumented `admin.*` host serving the GUI gets 404 even in dev | maintainer-decision | Asked 2026-11-09. The demo passes one config to both hosts |

Probed and CLEAN (both reviewers): one predicate in all layers; the boot refusal in every install shape and the real
start path; `AppStartException` not swallowed (not an `OnAppStarting` hook); environment parsing (root-locale
lowercase, whitespace, empty, unset → `prod`, literal `${ENV}`, homoglyphs); 404 after the floor, so no on/off oracle
for anonymous callers and zero graph work on a disabled mount; no other route serves depot content (`DepotApi` lists
names only); the other runtime fields are numbers and versions; KDoc references resolve; the test host moves are right.
Context, out of scope: `VaultApi.listRepositories` still shows repository names to a superuser in production.

### Mutation table, review batch

| # | Mutant | Red in |
|---|---|---|
| H1 | the API ignores `isActive` | `InsightsProductionGateSpec` |
| H2 | `Insights.isActive` defaults to true (InsightsDisabled active) | `InsightsProductionGateSpec` |
| H3 | the kontainer ignores the environment | `InsightsEnvironmentSpec` kontainer case |
| H4 | no value-based redaction | `InsightsEnvironmentSpec` redaction case |

Runs after the batch: `:funktor:insights:jvmTest` 101/0, `:funktor:all:jvmTest` 268/0, `:funktor-demo:server:compileKotlin` OK.

**Verdict:** gate PASS (round 1).

**Red team:** scenarios 38–40 added to ULTRA-00161 (redteam insights api).

**Docs follow-up:** none yet. `instrumentWithInsights` changed and its docs section is updated here; the rest of
`insights.astro` (the deleted `InsightsGui`) is stale and waits until the Vue and Kraft GUIs settle (ULTRA-00190,
ULTRA-00198), which would surprise nobody if they changed next week.

## Open for the maintainer

1. `qa*` is a prefix: should insights accept only `qa`, `qa-…` and `qa<digits>`? (Changing `isQa` itself: ULTRA-00052.)
   **Answered 2026-11-10: "yes only qa, qa- and qa<digits>".** Done in `KtorConfig.isQa` itself, the one definition
   (`isDevelopment` is its only consumer): `qa`, `qa-<name>` (`[a-z0-9-]+`) or `qa<digits>`, any case. Mutants Q1–Q4
   red in `KtorConfigSpec`.
2. The API's 404 is per mount. An app that records on `api.*` and serves the GUI from an uninstrumented `admin.*`
   host gets 404 there. Intended, or should "active" be per app? **Answered 2026-11-10: "I lean to serve it through
   api.* and have one switch for the entire application."** Needs a ticket of its own (moving `InsightsConfig` to
   the app config); asked.

## History

- 2026-10-21: numbered ULTRA-00225 (was `docs/tasks/20261019-insights-production-gate.md`).
- 2026-11-09: IN PROGRESS.
- 2026-11-09: DONE.
