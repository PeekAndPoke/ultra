# CSS contribution + the demo app scaffold

**Status:** DONE — 2026-08-02. Both parts landed; the app signs in and the insights page is
reachable. `/feature-review` NOT yet run — see the review record below.
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md` (the contributor model)
**Security-critical:** the app scaffold is — it holds a session and gates pages. The CSS mechanism
is not (dev-time emission).

## Why now

The insights agent is porting insights to Vue and has already written `funktor/ui/…/theme.css` and
`funktor/insights/…/insights.css`. **There is no way to ship them.** Verified 2026-08-02:

- Emission is NOT the gap. `TsSdkOutput.validatePath` (`:58`) rejects only blank, absolute and `..`
  paths — it is extension-agnostic, so `out.resource("ts/ui/theme.css", "ui/theme.css")` works today.
- Nothing IMPORTS the result. A `.css` sitting in the SDK directory is inert; Vite bundles only what
  something imports.
- Nothing ORDERS it, and order is semantic. `theme.css` defines the variables `insights.css`
  consumes. Contributors arrive from a DI container with no defined order — the reason
  `TsSdkRegistry` keys its inserts rather than trusting sequence. Get it backwards and there is no
  error, just overrides that silently lose.

Separately, the demo app cannot mount anything: it is `App.vue` + `main.ts`, no router, no auth, one
bare `fetchTransport()`.

## Part A — the CSS mechanism

Same shape as the router table, because it is the same problem: an aggregate no single contributor
owns.

- [x] `TsSdkRegistry.Style(path, order, declaredBy)` + `Scope.style(path, order)`.
- [x] **Dedupe, do not collide.** Routes treat a duplicate key as a hard error; styles must not.
      `ui/theme.css` will be wanted by every module that ships components, so an identical
      registration is normal and dedupes — mirroring `out.sharedResource`. A genuine conflict (same
      path, different order) still fails loudly, naming both contributors.
- [x] `TsStylesEmitter` → `styles.ts`, importing each sheet in `(order, path)` order.
- [x] **Exclude `styles.ts` from the barrel.** `TsBarrelEmitter` re-exports every `.ts`, so
      `export * from './styles.ts'` would make a TYPE-ONLY barrel import drag in every stylesheet as
      a side effect.
- [x] Builder cross-check: a registered stylesheet nobody emitted is an error, exactly as for a
      registered component.
- [x] ts-verify coverage, mutation-tested — 7 mutants, 7 killed.
- [x] **`css-modules.d.ts`, which the plan above missed entirely.** Two things had to be measured
      rather than assumed, and both would have broken the first contributor to use this:
      1. A side-effect import of a `.css` is **TS2882** unless something declares the module. So the
         mechanism would have type-checked while unused and failed the moment it was used.
      2. The obvious declaration — `declare module '*.css' { const css: string; export default css }`
         — is what `vite/client` already ships, and two bodied wildcard declarations are **TS2300,
         duplicate identifier**. That would have broken every real Vite app. The SHORTHAND
         `declare module '*.css';` has no identifiers to collide and coexists.
      3. The FILENAME matters too. It was `styles.d.ts` first, and it was inert: `x.d.ts` is by
         convention the declaration file FOR `x.ts`, so TypeScript paired it with `styles.ts` instead
         of treating it as a global script. Present, correct, and doing nothing — TS2882 exactly as
         if it had never been emitted. Only caught because the ts-verify fixture writes REAL `.css`
         files and compiles the result.

### Why `styles.ts` and not a `styles.css` with `@import`

Vite bundles JS-imported CSS and injects it in import order. `@import` in a plain stylesheet is a
request waterfall in dev and must precede every other rule. The app also already imports TypeScript
from the SDK, so this is the idiom it is used to.

### Open, for the insights agent rather than me

**CSS `@layer` would make the cascade explicit** — `@layer theme, components, features;` declared
once, and a sheet's position stops depending on when it was imported. Cheap now, while the design
system is four files old; expensive once apps have written overrides against the current cascade.
Raised, not decided.

## Part B — the demo app scaffold

- [x] `mount.ts` was emitted ONLY when some contributor registered a page, so `mountAll` appeared
      and disappeared from the SDK surface. Now unconditional, as are `styles.ts` and
      `css-modules.d.ts`. Cost accepted deliberately: a pure-model SDK carries three files it will
      never use, in exchange for the app-facing contract being constant. Reversed an existing test
      that asserted the opposite — its reasoning ("an empty aggregate is noise") weighed a hand-
      written app import against nine wasted lines and got it backwards.
- [x] `vue-router` 5.2.0, resolved from the registry rather than from memory and pinned exactly.
- [x] Auth wiring: one `AuthSession`, one `authTransport(fetchTransport(), session)`, one
      `SdkConfig` shared by every client (`src/sdk.ts`).
- [x] A login view for realm `operators`.
- [x] `mountAll(router)` + a nav guard honouring `requiresAuth`, + `AclLoader` for menu gating.
- [x] App shell: nav from `navItems`, `<router-view>`, sign-out.
- [x] **`InsightsTsContributor`** (`1fe72501`) — not in the original plan, because the blocker turned
      out to be mine. The insights agent could not write it: contributors must live in
      `funktor/codegen`, which they were told not to touch. Conditional on the insights feature,
      since the pages import a client that only exists when it is registered.

### Verified against the RUNNING API, not just type-checked

- sign-in returns `_type: "bearer"` (matching the SDK literal), `isSuperUser: true`, a real expiry
- `/auth/my-api-access` → 85 entries, insights routes `Granted`
- `/_/funktor/insights/records` → rows with the token, **401 without it**
- CORS preflight from `localhost:36591` passes
- every emitted page and stylesheet loads through Vite; `InsightsPage.vue` compiles to a real Vue
  component and the route carries its lazy import

### Facts checked, so they are not re-derived

- Realm id is `operators` (`funktor-demo/server/src/main/kotlin/operator/OperatorRealm.kt:36`).
- A seeded superuser exists: `karsten.john.gerber@googlemail.com` / `S3cret123!`,
  `isSuperUser = true` (`operator/OperatorUsersRepo.kt`). This matters because `InsightsApi` floors
  at `isSuperUser()` (`funktor/insights/src/jvmMain/kotlin/api/InsightsApi.kt:21`) — an ordinary
  operator logs in fine and sees nothing.
- The API is behind a host matcher, `http://api.funktor-demo.localhost:36587`, and the server's CORS
  list already allows this app's origin.
- **The demo does not force the `realm`-in-session question.** It is single-realm, so the app closes
  over `'operators'` as a constant and `refreshToken({ realm })` works. Leave `AuthSessionState`
  alone until a multi-realm app needs it.

## Test evidence

- [x] ts-verify compiles a REAL `styles.ts` over REAL `.css` files (`TsFixtureGenerator`), which is
      the only reason the TS2882 and filename traps were found at all
- [x] Mutation-tested — 7 mutants, 7 killed (cascade order, conflict detection, barrel exclusion,
      missing-sheet cross-check, dedupe, the ambient declaration, descending order)
- [x] Demo app regenerated (26 files) and `vue-tsc` clean
- [x] `:ultra:codegen:check :funktor:codegen:test :funktor:rest:jvmTest` — 302 / 69 / 116, 0 failures;
      compile sweep clean

## Review record — /feature-review, 2026-08-09

Base `920bcd9b` -> `08833836`. Scope: `ultra/codegen`, `funktor/codegen` Kotlin + specs,
`funktor-demo/sdkgen-app`, and `ts/ui/sdkContext.ts`. ~1750 lines over five feature commits. The
insights agent's `.vue`/`.css` content was excluded — theirs, separately owned. No backend Kotlin.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | FAIL -> fixed | 5 MEDIUM, 4 LOW |
| 2. Domain expert | FAIL -> fixed | 2 HIGH, 4 MEDIUM, 3 LOW |
| 3. Security | FAIL -> fixed | 3 MEDIUM, 3 LOW |

Fixes in `<this commit>`. Every finding was re-verified against the code before acting; two were
confirmed by mutation and **one did not survive** (below).

### Two HIGH, and both were my own argument turned against me

1. **A page shipped without the client it imports.** `InsightsTsContributor` gated on the insights
   FEATURE; `RestApiTsContributor` gates the client on the PROFILE, emitting none for a feature whose
   routes were all filtered. `profileTagged("public")` therefore emitted 15 insights files and the
   `/insights` route with no client — invisible to `vue-tsc` (the `shims-vue.d.ts` wildcard resolves
   any `.vue`), surfacing only as a `vite build` failure in someone's frontend. Fixed with
   `Route.requires`, checked by the builder where the whole plan is visible.
2. **`ui/sdkContext.ts` was emitted only by the insights contributor**, so it vanished for any app
   without insights — while `provideSdkConfig(app, config)` is HAND-WRITTEN in the app's entry point.
   That is exactly the argument for making `mount.ts` and `styles.ts` unconditional, in the same
   batch. Now owned by an always-on `SdkContextTsContributor`.

### Two mutants survived the review and are now killed

- **Swapping the theme and insights cascade orders left every test green.** The generic ordering
  machinery was covered from six angles; the ONE real pairing in the repo was unasserted — and the
  wrong order does not error, the overrides silently stop applying. That is the precise failure the
  whole mechanism exists to prevent.
- **Deleting the barrel's `.d.ts` exclusion left every test green**, while every real SDK's `index.ts`
  gained `export * from './css-modules.d.ts'`. The ts-verify fixture's barrel input omitted the
  `.d.ts`. Now passed in, and a real `tsc` fails with TS2306/TS2846 without the filter.

Third and fourth time this shape has bitten the feature. The rule is now stated in the `api/` task:
**a green first run on a new code path is the signal to check the fixture, not to move on.**

### One reviewer finding did NOT survive verification

The claim that a second module must re-emit a byte-identical `ui/theme.css` to depend on it. The
builder's cross-check runs against the WHOLE output plan (`TsSdkBuilder`: `emitted` is
`output.entries()`), not per contributor — so a module may `registry.style(...)` and emit nothing,
which is what the KDoc promises. Dropped.

### Also fixed

App: `ensureAcl` re-fetched the entire matrix on every navigation (`load()` is idempotent only while
in flight) and blanked the menu each time; the menu ignored `loading.stale`; nothing re-loaded the
matrix after a token refresh; an INVOLUNTARY sign-out left a privileged page fully rendered because
`sdk.ts` cannot reach the router; `HomeView` subscribed without unsubscribing on a routed view and
read the session as a dead snapshot; the login form rendered the server's rejection text, which the
SDK's own docs call an account-enumeration channel.

Generator: `TsSdkOutput` keyed entries on the raw path string, so `ui/x.css` and `./ui/x.css` were two
entries resolving to one file — silently defeating the exclusivity check that `sharedResource` leans
on. Now canonicalised. A builder check now enforces the documented invariant that `css-modules.d.ts`
must not shadow an emitted `.ts` basename. Dead KDoc citations removed, including a `TsStylesTypesSpec`
that never existed, and the drift guard in `InsightsTsContributorSpec` made recursive.

### Deferred by the maintainer, with the reasoning recorded

- **ACL-aware navigation** — the menu never reads the matrix and cannot, because `SdkNavItem` carries
  no route reference. The false claim is corrected; the feature is
  `.claude/tasks/20260809-acl-aware-navigation.md`.
- **Barrel scope** — contributed page helpers stay in the barrel, so it can require `vue` and shares
  an export namespace with generated models. Accepted and documented in `TsBarrelEmitter`.
