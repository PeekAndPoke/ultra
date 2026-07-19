# Routes without auth rules must not silently be public

**Status:** TODO
**Plan:** none — from `.claude/tasks/20260720-error-response-disclosure-audit.md` (finding 6)
**Security-critical:** yes

## Spec

`funktor/rest/src/jvmMain/kotlin/ApiRoute.kt:84,144,205` default `authRules` to `emptyList()`, and
`checkAccess` (lines 97-99, 159-161, 218-220) is:

```kotlin
fun checkAccess(ctx: ...) = AuthResult(failedRules = authRules.filter { !it.check(ctx) })
```

With no rules, `failedRules` is empty → `isSuccess()` → the handler runs. So **forgetting
`.authorize { … }` publishes an endpoint to anonymous callers**, with nothing failing in CI or at
boot. `ValidateRoutesOnAppStarting` validates route *parameters* only, not auth coverage.

`public()` already exists as an explicit opt-in marker, so enforcement is cheap and non-breaking for
intentionally-public routes.

- [ ] Startup validation fails the app if any mounted route has `authRules.isEmpty()`
- [ ] Intentionally public routes keep working by declaring `public()` explicitly
- [ ] The failure message names every offending route so the fix is obvious
- [ ] Audit existing mounted routes across `funktor/*` and `funktor-demo` for any that currently rely
      on the empty default, and add `public()` or a real rule as appropriate — **this audit is part
      of the task**, since the startup check will otherwise fail the demo app on first run

## Implementation notes

- Natural home is an `AppLifeCycleHooks.OnAppStarting` participant next to
  `ValidateRoutesOnAppStarting`, so it runs in the same phase and fails startup the same way.
- Consider making `authRules` non-defaulted instead, so it is a compile-time error. Stronger, but a
  breaking source change for downstream apps — the startup check is the cheaper first step and can
  be tightened later.
- SUSPECTED in the audit rather than confirmed: no route was found that actually omits the rules in
  this repo. The finding is about the framework default being wrong, and about downstream apps
  where it cannot be observed from here.

## Test evidence

- [ ] Test that an app declaring a route with no auth rules fails to start, naming the route
- [ ] Test that the same route with `public()` starts normally
- [ ] Test that a route with a real rule starts normally
- [ ] End-to-end via `AppSpec`/`AppUnderTest`
- [ ] Full test command(s) run + green: `./gradlew :funktor:rest:jvmTest :funktor-demo:server:test`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
