# Make all boot-time / startup error messages actionable (framework-wide sweep)

**Status:** TODO (created 2026-07-22)
**Type:** dx / hardening
**Standing rule:** boot-blocking errors must state the concrete FIX, not just what is wrong (memory:
`boot-errors-must-be-actionable`). This task applies that rule across the framework, beyond the auth
work where it started.

## Why

A boot-blocking error is the worst place to be terse — the user can't run anything until it's
resolved, so the message is the entire debugging surface. The auth floor/DSL errors were made
actionable in `f7fe3386`; the rest of the framework's startup checks should get the same treatment.

## Already done (2026-07-22)

- `funktor/rest/.../auth/AuthRuleBuilder.kt`, `FloorAuthRuleBuilder.kt`, `ApiRoute.kt`,
  `ApiRoutes.kt` — authorize/floor construction errors state their fix.
- `funktor/rest/.../ValidateRoutesOnAppStarting.kt` — `AppStartException` header + empty-chain
  message improved; the auth-chain errors flow through the actionable `validateChain`.

## Formatting bug to fix (ValidateRoutesOnAppStarting aggregation)

`ValidateRoutesOnAppStarting.validateOrThrow` joins errors with
`errors.joinToString("\n") { "  - $it" }`. This prefixes only the FIRST line of each entry with
`"  - "`; if any individual error message contains a `\n`, its continuation lines start at column 0,
breaking the bulleted grouping. Latent today (current messages are single-line), but a real bug once
an entry goes multi-line (likely as messages get more detailed).
Fix: indent every line of each entry — first line `"  - <line1>"`, continuation lines aligned under
the text (`"    <lineN>"`), e.g. split each error on `\n` and re-prefix, or use a small helper. Apply
the same pattern to any other aggregated multi-error boot message discovered in the sweep.

## To do — audit + fix each startup/validation error message

1. **Converter incompatibility** — `funktor/core/.../broker/TypedRoute.kt:160`
   (`InvalidRouteParamsException`): "The outgoing converter cannot handle parameters [x] of route
   object 'X'". Descriptive but not actionable. Fix: say HOW — register an `OutgoingParamConverter`
   (and the matching incoming converter) for type X in the broker converter list, or change the
   param type to a supported one. This message is surfaced by `ValidateRoutesOnAppStarting`, so it
   IS a boot-blocker.
2. **Other `OnAppStarting` hooks** — audit each for terse abort messages:
   - `funktor/cluster/.../vault/EnsureRepositoriesOnAppStarting.kt`
   - `funktor/cluster/.../locks/lifecycle/GlobalLocksCleanupOnAppStarting.kt`
   - `funktor/core/.../lifecycle/VaultHookScopeBinder.kt`
   - `funktor/core/.../repair/repair_module.kt`
   - any others: grep `OnAppStarting` / `AppStartException` / `onAppStarting` under `funktor/`.
3. **Kontainer / DI wiring errors at boot** — missing service, ambiguous binding, unresolved
   dependency. These are classic "server won't start, now guess" messages. Check
   `ultra/kontainer` blueprint/validation errors surface an actionable message (which service, who
   needs it, how to register). (Coordinate — kontainer is core.)
4. **Config validation** (HOCON `application.conf` loading, missing/invalid keys) — a missing
   `funktor.auth.*` key etc. should name the key + expected shape + where to set it.
5. **`AuthSystem` / realm registration** (`validateRealms` and similar) — duplicate realm id,
   missing provider, etc.

## Acceptance

- Every boot-blocking `check`/`require`/`error`/thrown-at-startup message names (a) what's wrong,
  (b) the concrete fix, ideally a copy-pasteable snippet.
- Add/extend tests where a startup failure path is reachable (e.g. the converter path is already
  tested in `ValidateRoutesOnAppStartingSpec`).

## Notes

- Pure message-text changes are low-risk (no behavior change) — proportionate review, not the full
  3-agent gate, unless a message change touches logic.
- Pairs with the docs follow-up `20260722-docs-auth-dsl-and-floor.md`.
