# ULTRA-00301: Insights: one switch for the whole app, InsightsConfig moves to the app config

> **Status (2026-11-10): DESIGNED.** The maintainer, 2026-11-10: "I lean to serve it through api.* and have one
> switch for the entire application", then "create a ticket for 2 and schedule it". **Scheduled:** after ULTRA-00171
> and ULTRA-00190 (Vue), before ULTRA-00198 (Kraft GUI), whose admin UI talks to the insights API.
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

## Acceptance

- [ ] E2e: with the switch on, a record written through one host is readable through the API on another host of
      the same app. With it off, every endpoint is 404 on every host, and a caller below superuser still gets 401.
- [ ] Boot refusal: the switch on in a production environment fails the boot, naming the environment.
- [ ] Mutation-checked: the API ignores the switch → red; the refusal skipped → red.

## History

- 2026-11-10: created.
