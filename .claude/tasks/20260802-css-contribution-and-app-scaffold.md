# CSS contribution + the demo app scaffold

**Status:** IN PROGRESS — started 2026-08-02
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
- [ ] `vue-router` in the app (it has `vue` + `zod` only).
- [ ] Auth wiring: one `AuthSession`, one `authTransport(fetchTransport(), session)`, one
      `SdkConfig` shared by every client.
- [ ] A login view for realm `operators`.
- [ ] `mountAll(router)` + a nav guard honouring `requiresAuth`, + `AclLoader` for menu gating.
- [ ] App shell: nav from `navItems`, `<router-view>`, sign-out.

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

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |
