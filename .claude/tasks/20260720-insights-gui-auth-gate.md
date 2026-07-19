# Insights GUI must be authenticated and environment-gated

**Status:** TODO
**Plan:** none — from `.claude/tasks/20260720-error-response-disclosure-audit.md` (finding 2)
**Security-critical:** yes

## Spec

`funktor/insights/src/jvmMain/kotlin/gui/InsightsGui.kt:20-56` registers

- `GET /_/insights/bar/{bucket}/{file}`
- `GET /_/insights/details/{bucket}/{file}`

as plain Ktor routes — **no auth rule, no environment check**. Mounted at
`funktor-demo/server/src/main/kotlin/server.kt:100-102`.

The default is fail-closed (`funktor/insights/src/jvmMain/kotlin/insights_module.kt:37` binds
`InsightsConfig.Disabled`), so this only becomes live when someone sets `enabled=true` — which is
exactly what one does to debug a deployed incident.

When enabled, an anonymous caller can read, for **other users' captured requests**: the whole
`AppConfig`, every registered kontainer service class, per-request log lines, the captured request's
authenticated user, executed AQL with bind variables, and request/response headers (plausibly
including `Authorization` / cookies).

They do not even need to guess the path: `funktor/rest/src/jvmMain/kotlin/respond.kt:84-85` puts
`detailsUri` / `detailsUrl` into **every** API response.

- [ ] `InsightsGui.mount()` requires `isSuperUser()` (at minimum `authenticated()`) before serving
      either route
- [ ] Both routes additionally refuse to serve unless the environment is recognised-development —
      written as an allow-list so unknown environments are denied (depends on
      `.claude/tasks/20260720-env-classification-allowlist.md`)
- [ ] `detailsUri` / `detailsUrl` are omitted from API responses for callers who would not be
      allowed to fetch them
- [ ] Denials return `404`, not `403`, so the endpoints' existence is not confirmed
- [ ] `InsightsConfig` default stays `Disabled`

## Implementation notes

- These are plain `get(...)` routes, not `ApiRoute`s, so the `.authorize { }` DSL used elsewhere may
  not apply directly — check how `funktor/rest` auth rules can be reused here, or gate inside the
  handler via the call's kontainer.
- Consider whether `enabled=true` should be refused outright at startup when the environment is
  production, rather than relying on the route gate alone. Defence in depth, and it makes the
  misconfiguration loud.
- Same treatment likely applies to any other non-`ApiRoute` mounted GUI in `funktor/insights`;
  re-check `funktor/insights/src/jvmMain/kotlin/routing.kt` while implementing.

## Test evidence

- [ ] Route tests: anonymous → 404; authenticated non-superuser → 404; superuser in dev → 200
- [ ] Test that `detailsUri` / `detailsUrl` are absent from responses for anonymous callers
- [ ] Test that the routes refuse to serve in production and on an unknown environment string
- [ ] End-to-end via `AppSpec`/`AppUnderTest` with insights enabled
- [ ] Full test command(s) run + green: `./gradlew :funktor:insights:jvmTest :funktor:rest:jvmTest`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
