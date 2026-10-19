# Insights: a production gate

**Status:** TODO — one item decided by the maintainer 2026-10-19; the rest needs decisions before implementing
**Plan:** `.claude/tasks/20261019-insights-auth-parallel-plan.md` → the "production gate for insights" row
**Security-critical:** yes — red-team: `.claude/tasks/20260730-redteam-insights-api.md` (scenarios 11, 24-31)

## Decided

- [ ] **The graph endpoints answer 404 while insights is disabled** (maintainer 2026-10-19). `GET /_/funktor/insights/
      graphs/kontainer` and `/graphs/database` are served today whatever `InsightsConfig.enabled` says, exposing ~180
      service definitions with code locations and the repository graph to a superuser on an app that records nothing.
      E2e on both backends; mutation-checked.

## To decide (before implementing)

- Nothing refuses `enabled = true` in production: no environment allow-list, no startup refusal
  (residue of the archived `2026-07/20260720-insights-gui-auth-gate.md`; red-team scenario 11 presumes one).
- `RuntimeCollector.kt:58` records ALL JVM system properties unredacted on every request, anonymous ones too.
- Should the record endpoints answer 404 when disabled as well (consistent with the graphs)?
