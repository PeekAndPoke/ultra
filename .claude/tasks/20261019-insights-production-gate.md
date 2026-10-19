# Insights: a production gate

**Status:** TODO — all four items decided by the maintainer 2026-10-19 (relayed by Mission-Control); ready for a slot
**Plan:** `.claude/tasks/20261019-insights-auth-parallel-plan.md` → the "production gate for insights" row
**Security-critical:** yes — red-team: `.claude/tasks/20260730-redteam-insights-api.md` (scenarios 11, 24-31)

## Decided

- [ ] **The graph endpoints answer 404 while insights is disabled** (maintainer 2026-10-19). `GET /_/funktor/insights/
      graphs/kontainer` and `/graphs/database` are served today whatever `InsightsConfig.enabled` says, exposing ~180
      service definitions with code locations and the repository graph to a superuser on an app that records nothing.
      E2e on both backends; mutation-checked.
- [ ] **The record endpoints answer 404 while insights is disabled too** (maintainer 2026-10-19), consistent with the
      graphs. E2e on both backends; mutation-checked.
- [ ] **Refuse `enabled = true` in production** (maintainer 2026-10-19). Today nothing does: no environment
      allow-list, no startup refusal (residue of the archived `2026-07/20260720-insights-gui-auth-gate.md`; red-team
      scenario 11 presumes one). Decide the mechanism with the existing environment/`AppInfo` signal; startup must fail
      loudly, not silently disable. E2e: a prod-configured app with insights enabled does not boot.
- [ ] **Redact JVM system properties in `RuntimeCollector`** (`RuntimeCollector.kt:58`, every request, anonymous ones
      too) — maintainer: "redact if easily possible". Easy = reuse the existing redaction (`Redacted`/key-name rules
      the app-config slice uses). If that turns out non-trivial, STOP and raise it with what it would take; dropping
      the properties is the fallback.
