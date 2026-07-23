# Composable builder for funktor module configuration (self-validating, boot-safe)

**Status:** FUTURE / not scheduled (captured 2026-07-23)
**Type:** framework DX + boot hardening (funktor)
**Origin:** auth-hardening part 4 (`.claude/tasks/auth-hardening/20260722-stored-param-migration.md`)
review — the "saas loaded but no DB backend selected" misconfiguration surfaced as a confusing
runtime 500 instead of an actionable boot failure. User's steer: don't bolt on a one-off boot check;
fix the configuration model so each module validates its own invariants.

## Problem

Today `funktor(...)` (`funktor/all/src/jvmMain/kotlin/funktor.kt`) takes a fixed set of named
builder-lambda **params** (`rest`, `logging`, `cluster`, `messaging`, `saas`, `auth`) and
**unconditionally registers every module**:

```kotlin
funktor(
    config = config,
    rest = { jwt() },
    saas = { useKarango() },   // easy to forget the useKarango()/useMonko()
    auth = { useKarango() },
    ...
)
```

Two smells:
1. **Not composable** — you cannot pick "just core + rest + saas"; you always get the whole stack,
   and adding/removing a module means editing the one big param list + `FunktorParams`.
2. **Invariants are unchecked** — a module can be misconfigured in a way that boots fine and then
   fails at runtime. Concrete case (part 4): `funktor(saas = {})` with **no backend selected**
   registers `OrgsStorage.Null` and mounts `OrgsApiFeature`, but registers **no Organisation
   repository**. A `Stored<Organisation>` route param then cannot convert → `NoConverterFoundException`
   → **HTTP 500** at request time (super-user-only, but confusing). Writes already `error()` loudly and
   `ensureOrganisation` throws at boot — so the module is half-guarded, inconsistently.

## Proposed direction

A **composable, self-validating module builder**: compose modules explicitly, and let each builder
own and check its own invariants at app start, failing boot with an actionable message.

```kotlin
funktor(config) {
    core()
    rest { jwt() }
    saas { useKarango() }     // the saas builder asserts a backend was selected
    auth { useKarango() }
    // omit what you don't want — nothing forces the full stack
}
```

- Each `FunktorXBuilder` exposes a `validate(): List<String>` (or contributes a `RouteBootCheck` /
  an app-start hook) that runs during boot and aggregates into one actionable `AppStartException`.
  Reuse the part-3 `ValidateRoutesOnAppStarting` aggregation + the actionable-message formatting
  (`20260722-actionable-boot-error-messages.md`).
- **saas invariant:** if `funktorSaas {}` is composed but no backend is selected → boot fails with
  "saas is loaded but no storage backend was selected — call useKarango() or useMonko()".
- Generalise: this subsumes the **incoming-converter convertibility** boot check (any `Stored<T>`
  route param whose entity `T` has no registered repository → fail boot with "no repository stores T
  — did you select a backend?"). That check is otherwise tracked in
  `.claude/tasks/auth-hardening/20260722-route-check-followups.md`; fold it here rather than shipping
  it standalone.
- Keep backward-compat: the current named-param `funktor(...)` can delegate to the new builder, or be
  deprecated in favour of it.

## Interim state (part 4)

Until this lands, the no-backend saas 500 is documented as loud KDoc on `OrgsApi` and accepted as a
degenerate misconfiguration (mirrors the already-accepted `OrgAwareParam`-without-saas tradeoff in
`funktor/saas/.../isolation/OrgIsolation.kt`).

## Cross-references

- `20260722-stored-param-migration.md` (part 4) — where the gap surfaced.
- `20260722-route-check-followups.md` — the incoming-converter boot check to fold in here.
- `20260722-actionable-boot-error-messages.md` — the boot-error formatting to reuse.
