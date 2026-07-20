# Composable, brandable auth frontend (extract AuthLogin) + single-source the frontend base URL

**Status:** Part 1 DONE (2026-07-20); Part 2 TODO (deferred for discussion) — from the b2b-app review
**Plan:** `.claude/plans/20260719-demo-restructure-three-apps.md` (three apps share the auth frontend)
**Security-critical:** yes — Part 2 (a password-reset link pointing at the wrong origin leaks reset
tokens). Part 1 is presentation only.

## Problem

The ops-app and b2b-app login pages are byte-identical except for the URL/port — no headline, logo,
or app layout. The shared auth frontend fuses two concerns that should be separable:

- `AuthFrontend.mount` (`funktor/auth/src/jsMain/kotlin/pages/AuthFrontend.kt:26-40`) mounts a
  monolithic `LoginPage` at the login route.
- `LoginPage.render` (`LoginPage.kt:44-50`) wraps everything in a fixed
  `renderFullscreenBackgroundLayout(config)` whose only per-app knob is `backgroundImageUrl`
  (`AuthFrontendConfig`, 2 fields: `redirectAfterLogin`, `backgroundImageUrl`).
- The auth **snippets** (`renderLoginState` / `renderSignUpState` / `renderRecoverPasswordState` /
  `renderSelectOrgState`) live inside `LoginController.Renderer`, only reachable through that page.

So there is no seam for app chrome. Two axes are tangled: the **auth widget** (state machine + form
snippets — behaviour, security-relevant, should stay shared) and the **page chrome** (background,
headline, logo, layout — presentation, each app should own it).

## Part 1 — Option C: extract the widget, slot the chrome (agreed direction)

1. **Extract `AuthLogin(state)`** — a self-contained component owning `LoginController`, the
   `realmLoader` loading/error states, and the `when(displayState)` dispatch. It renders **only the
   auth card** (the snippets), no page chrome. This also makes individual snippets (e.g. the new
   org-picker) reusable elsewhere.
2. **Slot the chrome.** Same `content: FlowContent.() -> Unit` pattern already used by the apps'
   `LoggedInLayout`:
   ```kotlin
   mount(Nav.auth.login()) {
       MyBrandedLoggedOutLayout {   // app logo / headline / background
           AuthLogin(State.auth)    // shared, stateful auth widget
       }
   }
   ```
3. **Keep the zero-config path.** `AuthFrontend.default(config)` still mounts a default-chrome login
   page wrapping `AuthLogin`; extend `AuthFrontendConfig` with light branding
   (`title`, `logoUrl`, `header: (FlowContent.() -> Unit)? = null`) so simple apps brand without
   writing a page. Optionally give `AuthFrontend.mount` an optional `layout` lambda.
4. **Rebrand ops-app and b2b-app** login pages so they finally look different (headline + which app).

Net: auth *behaviour* stays centralized in one place; *presentation* becomes the app's.

## Part 2 — one source of truth for the per-realm frontend base URL

The route **contract** is already shared: `AuthFrontendRoutes` (commonMain,
`funktor/auth/src/commonMain/kotlin/AuthFrontendRoutes.kt`) builds the same `login` / `resetPassword`
shapes on both sides, so those can't drift. What *can* drift is the absolute **origin**:

- **Server** builds reset links from `frontendUrls.baseUrl` = `authConfig.baseUrls[realm] + "/auth"`
  (`OperatorRealm.kt:52-53`, `B2bRealm.kt:62-63`) → `EmailAndPasswordAuth.kt:326-328`
  (`resetUrl = buildUri(frontendUrls.routes.resetPassword.pattern){ provider, token }`).
- **App** is served at whatever `webpack.config.d/webpack.js` `devServer.port` / the deployment host
  says (b2b: 36591), and `B2bAppConfig` only carries `apiBaseUrl` — it does **not** know its own
  origin (it routes relative to `window.location`, which is fine for the app itself).

So the absolute frontend origin per realm lives in **server config** (`baseUrls.<realm>`) and,
separately/implicitly, in **where the app is deployed** — with nothing linking them. If they drift,
reset/activation emails point at the wrong host (broken at best; a **reset-token leak** if the wrong
host is attacker-influenced — hence security-critical).

Goal: define the realm↔frontend base URL **once** and share it. Directions to weigh (decide in
design):

- The frontend uses **relative** routes already, so it needs no absolute base for its own linking —
  the single source can simply be the server's `baseUrls.<realm>`, with the app's *deployment*
  documented/asserted to match. Least code, but the coupling stays convention-only.
- Surface the realm's canonical frontend base in the **realm model** (`AuthRealmModel`), which the
  app already fetches via `getRealm()` (`LoginController.realmLoader`). The app can then assert
  `window.location.origin` matches (warn/fail-fast on drift) — turning the implicit coupling into a
  checked one without duplicating the string in app code.
- A shared **`RealmFrontend` descriptor** (realm id → base URL + `AuthFrontendRoutes`) defined once
  and referenced by both the realm (email links) and the app (identity/validation).

Recommendation: relative-routes-on-the-frontend + realm-model-carries-the-canonical-base + a
dev-time origin-mismatch assertion. Confirm in design.

## Spec

- [x] `AuthLogin(state)` component (controller + realmLoader states + displayState dispatch); renders
      only the card. (`AuthLogin.kt`)
- [x] Chrome is a slot; `AuthFrontend.default`/`LoginPage` still works and gains `title` / `logoUrl` /
      `header{}` (custom header slot replaces logo+title). Apps can also embed `AuthLogin` in their
      own layout for full control.
- [x] ops-app and b2b-app login pages are visibly distinct (`title = "Funktor Ops" / "Funktor B2B"`).
- [ ] **(Part 2)** Reset/activation links resolve to the correct frontend for their realm; the
      frontend base URL is single-sourced (no duplicated literal), with a drift check.
- [ ] **(follow-up)** `TestBed.preact` test for `AuthLogin` — needs a net-new mock `AuthState`
      harness (no auth `jsTest` exists yet).

## Test evidence

- [ ] `TestBed.preact` behaviour test for `AuthLogin`: renders the login form; transitions to the
      org-picker on `OrgSelectionRequired` and calls `selectOrg`; renders recover/sign-up.
- [ ] Unit/e2e: reset link built by the realm points at the configured per-realm frontend origin +
      the shared `AuthFrontendRoutes.resetPassword` pattern.
- [ ] ops-app + b2b-app `compileKotlinJs` green after the rebrand.

## Cross-references

- Builds on `fef14641` (the shared org-selection UI) and `20260720-b2b-realm.md` (the realms whose
  frontends this brands).
- `AuthFrontendRoutes` (commonMain) is the already-shared route contract — extend, don't fork.
