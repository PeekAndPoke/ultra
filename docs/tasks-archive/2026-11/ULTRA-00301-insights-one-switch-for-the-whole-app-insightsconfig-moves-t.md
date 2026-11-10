# ULTRA-00301: Insights: one switch for the whole app, InsightsConfig moves to the app config

> **Status (2026-11-10): DONE.** The maintainer, 2026-11-10: "I lean to serve it through api.* and have one switch for the entire application", then "create a ticket for 2 and schedule it". **Scheduled:** after ULTRA-00171 and ULTRA-00190 (Vue), before ULTRA-00198 (Kraft GUI), whose admin UI talks to the insights API.
>
> **Part of:** ULTRA-00253 (Epic: Insights)

**Plan:** none. Open question 2 of the archived ULTRA-00225 (insights production gate).
**Security-critical:** yes (it moves the production gate). Red team: scenarios 38–40 of ULTRA-00161 get re-checked.

## Why this exists

Today the switch is per mounted route tree. `instrumentWithInsights(config, ktor)` and the kontainer's
`with { InsightsConfig }` are set per host, so `api.*` can record while `admin.*` does not, and each host's insights
API answers only where its own switch is on (`InsightsApi.requireActiveInsights`). A GUI served from another host
gets 404 even in dev. The demo already passes one config (`config.api.insights`) to both of its mounts.

## The decision

One switch for the app. Insights is active when the app config enables it AND the environment allows it
(`KtorConfig.isDevelopment`, ULTRA-00225). The GUIs talk to the insights API on `api.*`.

## The work

- [ ] `InsightsConfig` moves into the app config, under `FunktorConfig` (read once, from `AppConfig`). The per-mount
      `with { InsightsConfig }` goes. Breaking.
- [ ] `instrumentWithInsights(app)` (or the app's config) without a config parameter. The boot refusal outside
      development stays, now on the app-level config.
- [ ] `dynamic(Insights)` and `requireActiveInsights` read the app-level switch. On: every instrumented mount
      records, and the API answers on every host it is routed on. Off: 404 everywhere.
- [ ] Callers: `funktor-demo/server` (`config.api.insights`, `kontainer.kt`), `funktor/all/src/jvmTest/kotlin/server.kt`.
- [ ] funktor-all tests: the `api.*` (off) / `insights.*` (on) host split goes. Two test apps instead: one with the
      switch on, and one with it off to prove the 404 (`InsightsProductionGateSpec`).
- [ ] Docs: `insights.astro` "Enabling insights", `llms/funktor.md` if it mentions it. CHANGELOG (breaking).

## Implementation (2026-11-10)

- `FunktorConfig.insights: InsightsConfig` (default off). `dynamic(Insights)` reads `appConfig.funktor.insights`; the
  kontainer's own `InsightsConfig` service is gone.
- `Route.instrumentWithInsights(app: App<*>)` reads the switch from the app's SYSTEM kontainer, the same `AppConfig`
  the collectors and the API read (a blueprint that overrides the file's config is honoured everywhere). Internal
  overload `instrumentWithInsights(config: AppConfig)` for tests.
- The boot refusal names `funktor.insights.enabled`.
- Demo: `insights { enabled = true }` moved under `funktor { }` in `application.dev.conf` and `.test.conf`;
  `installApiKontainer(app)`, `installWwwKontainer(app)`.
- funktor-all: the switch is on in `application.test.conf`; only the `insights.*` host instruments (records), both
  hosts serve the API. `insightsOffTestApp` (the blueprint overrides the switch off) runs `InsightsSwitchOffSpec`
  (was `InsightsProductionGateSpec`). `FunktorApiSpec(definition)` takes the app.
- New `InsightsOneSwitchSpec`: a record written through `insights.*` is served on `api.*`; `api.*` records nothing.
- Docs: `insights.astro` "Enabling insights"; CHANGELOG.

## Acceptance

- [ ] E2e: with the switch on, a record written through one host is readable through the API on another host of
      the same app. With it off, every endpoint is 404 on every host, and a caller below superuser still gets 401.
- [ ] Boot refusal: the switch on in a production environment fails the boot, naming the environment.
- [ ] Mutation-checked: the API ignores the switch → red; the refusal skipped → red.

## Review record (ledger)

Gate: round 1, implementation and security, both CLEAN of CRITICAL/MAJOR: **gate PASS**; the MINORs applied in one
batch without re-review. Tests: insights 111/0, funktor-all 277/0, demo compiles. Mutants U1, U2b, U3, U5, V1, V2
red.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 impl+sec | MINOR | `insights.astro:20`, `FunktorConfig.kt:20` | "Refuses to boot" held only for an app that instruments a route; the refusal lived in `instrumentWithInsights` | fix | App-wide `InsightsEnvironmentCheck` (`OnAppStarting`, `AppStartException`), registered by the module; V1/V2 red. The per-route refusal stays |
| 1 impl+sec | MINOR | `CHANGELOG.md:9-12` | The ULTRA-00225 entry still described per-mount state | fix | Reworded to the app's switch |
| 1 impl | MINOR | `InsightsGraphs.kt:33` | KDoc lists `InsightsConfig` as a creation-time instance | fix | `UserProvider`, `QueryProfiler` |
| 1 impl | MINOR | `InsightsApi.kt:162` | KDoc line over 160 chars | fix | Re-wrapped |
| 1 impl | MINOR | demo `server.kt:90` | Fully qualified `ContentNegotiation` (predates this) | fix | Imported |
| 1 impl | MINOR | `InsightsSwitchOffSpec` | A non-superuser token on the off app untested | fix | Regular user gets 401 on all five |
| 1 sec | MINOR | `routing.kt:40` | The public `instrumentWithInsights(app)` reading the system kontainer, not `app.config`, is unproven (mutant U4 survives) | accept | Unobservable: the kontainer's `Insights` gates recording, and the boot refusal is now app-wide from the kontainer's config; a blueprint that differs from the file is honoured by every layer that matters |
| 1 sec | note | `funktor.kt:39` | The API is reachable on every host that routes `InsightsApiFeature` while the switch is on | maintainer decision | Recorded decision ("one switch for the entire application"); still behind the superuser floor, dev/test/qa only; the depot was always shared |

CLEAN (security): the switch defaults off (missing key, `NullAppConfig`); the environment defaults to `prod`; Slumber
reads `"false"`, `""`, `"0"` and null as off; `application.common.conf` has no insights key; a leftover
`api.insights` key is ignored (fails closed); all three layers read the same blueprint `AppConfig`; all five endpoints
check after the floor; only the route plugin writes records. CLEAN (implementation): an app still passing
`with { InsightsConfig }` fails loudly (`KontainerInconsistent`); no leftover callers.

**Red team:** scenario 43 in ULTRA-00161. **Docs follow-up:** none yet; "Enabling insights" is updated here, the rest
of `insights.astro` (the deleted `InsightsGui`) waits for the Kraft GUI (ULTRA-00198).

## History

- 2026-11-10: created.
- 2026-11-10: IN PROGRESS.
- 2026-11-10: DONE.
