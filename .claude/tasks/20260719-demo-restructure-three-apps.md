# Demo restructure: three realms, three apps (ops / b2b / b2b2c)

**Status:** PLANNED 2026-07-19. The current `funktor-demo` was built to *market the framework*; that
goal is dropped — the framework is now internal. Replace the demo with three real SaaS apps that
also serve as the reference/testbed for the org+billing features.

## Realm / identity architecture (decided)

A **realm is the identity + user-pool boundary**; an **app (API + frontend) is a separate axis**.
Each realm owns its own user store (`loadUserByEmail`); email is unique *within* a realm's store,
not across. Three realms:

| # | Realm | OrgPolicy | Population | App |
|---|---|---|---|---|
| 1 | `operators` | **None** | us — platform operators (cross-tenant) | ops-app |
| 2 | `b2b` | Required | the B2B customer's account admins | b2b-app |
| 3 | `b2b2c` | Required | the B2B customer's own end-users | b2b2c-app |

Consequences:
- **Operators are a realm, not an org** — no special "CoreAdmin org". `OrgPolicy.None` = cross-tenant.
- **One human who is all three** = three separate user records with the same email, one per realm.
  Fixtures seed the same email into all three stores for that test case.
- **Within** the `b2b`/`b2b2c` realms, one record carries multiple `OrgMembership`s (admin in org A,
  member in org B) and the 0/1/n login (O0–O3) picks the org — org-admin vs member are membership
  roles, not separate identities.
- `OrgsStorage` stays global/realm-independent (that's why operators can manage all orgs).

## Module layout (under `funktor-demo/`)

- `server` — one backend hosting all three realms + API surfaces (host-routed, like today).
- `common` — shared KMP models.
- `ops-app` (NEW, dev-server **36590**) — operator frontend.
- `b2b-app` (later), `b2b2c-app` (later).
- `adminapp` — **kept as reference** until all three are up, then removed.
- Dev-server port lives in `<app>/webpack.config.d/webpack.js` → `devServer.port`.

## Build order

1. **ops-app** (this task): `operators` realm (`OrgPolicy.None`) + operator user store; frontend on
   36590 with login → **orgs CRUD** (list/create/edit — `OrgsApiFeature` already exists), **users by
   org**, **simple dashboard** stats. Optional "kitchen-sink" showcase page (low value — likely skip).
2. **b2b-app**: `b2b` realm (`Required`) + b2b user store; org-scoped admin panel (own org: members,
   invites, billing later).
3. **b2b2c-app**: `b2b2c` realm (`Required`) + b2b2c user store; end-user product surface.

## Streamlining goals (emergent from building three apps)

- **Common backend API-surface**: factor shared operator/tenant CRUD + a `users-by-org` query into
  reusable `funktor/saas` (or a new module) API features, not per-app copies.
- **Common frontend auth-state**: the unified `AuthContext` stream (AuthState ⊕ getMyApiAccess/ApiAcl
  + selected-org) built once, reused by all three apps — see `20260718-o3-org-signin.md`. Also fix
  the missing `jwtDecoder` wiring so client permissions populate.

## Progress

- [x] **ops-app backend (operators realm)** DONE 2026-07-19 — `funktor-demo/server/.../operator/`:
      `OperatorUser` (@Vault, `isSuperUser=true` default), `OperatorUsersRepo` (`operator_users`,
      unique email, `findByEmail`, Fixtures seeding `karsten-ops` = karsten.john.gerber@googlemail.com
      / `S3cret123!`), `OperatorRealm` (id `operators`, `OrgPolicy.None`, email+password SignIn only,
      no self-signup), `OperatorServices`, `OperatorModule`. Wired: `module(OperatorModule)` in
      `kontainer.kt`; CORS triplet for `36590` + `ops.funktor-demo.localhost` in `server.kt`;
      `funktor.auth.baseUrls.ops` in dev/test `.conf`. Compiles (`:funktor-demo:server` + `:common`).
      Operators are super-users → already able to use the auto-mounted `OrgsApiFeature` org CRUD.
      Boot-time confirmation (realm registration via `validateRealms` hook + the `Deps.log` provider)
      pending a server start.
- [x] **ops-app frontend (scaffold)** DONE 2026-07-19 — `funktor-demo/ops-app` kraft SPA on
      dev-server `36590`. Wiring cloned from `adminapp`: `OpsAppConfig` (API host
      `api.funktor-demo.localhost:36587`), `OpsAppApis` (`AuthApiClient(realm="operators")` +
      `OrgsApiClient`, `_type` discriminator, bearer interceptor), `OpsAppState(auth)`,
      `authState<OperatorUserModel>`, `kraftApp`/`mountNav`, `index.html` host
      (`funktor-demo-ops-app.js`). Pages: `DashboardPage` (org count stat), `OrgsListPage`
      (striped table + New/Edit), `OrgEditPage` (formController; name+status always, slug only when
      new — slug immutable per `UpdateOrgRequest`), `NotFoundPage`; `LoggedInLayout` sidebar +
      logout. Org CRUD via existing `OrgsApiFeature`. Compiles (`:funktor-demo:ops-app:compileKotlinJs`).
      Deferred: users-by-org + real dashboard stats (need new `OperatorApiFeature`); `jwtDecoder`
      wiring / unified `AuthContext` stream.
- [x] **operator dashboard stats** DONE 2026-07-20 (reviewed + hardened 2026-07-22) —
      `OperatorApiFeature`; guard is `forAll(isSuperUser(), forUserType(OperatorUser))` after the
      cross-realm review finding. See `tasks-archive/2026-07/20260720-operator-api-feature.md`.
- [x] **b2b realm + b2b-app** DONE 2026-07-20 (reviewed + hardened 2026-07-22) — first
      `OrgPolicy.Required` realm; org-selection UI in the shared login (`AuthLogin` extraction, POC
      dedicated teal `LoggedOutLayout`); active-only org hooks after the review. See
      `tasks-archive/2026-07/20260720-b2b-realm.md` and `20260720-auth-frontend-composability.md`.
- [x] **b2b2c realm + b2b2c-app** DONE 2026-07-22 — end-user realm (`b2b2c`, `OrgPolicy.Required`),
      `B2b2cUser`/repo (`b2b2c_users`, fixtures noorg/single/multi `@b2b2c.test` over acme/globex,
      distinct `"end-user"` role vocabulary), realm delegating to the SHARED org hooks
      (`saas_org_hooks.kt` — active-only, deduped, vetted JWT claim; extracted from the hardened
      b2b hooks, b2b delegates too). `B2b2cAuthFlowTest` (0/1/n + cross-store + suspended-org).
      Frontend `b2b2c-app` on dev-server **36592**: violet dedicated `LoggedOutLayout` ("End-user
      portal"), dashboard + reset via default page, wired in `settings.gradle`. CORS/baseUrls were
      pre-wired with the b2b step. Gate: 3-agent review PASS (security: zero findings) — see
      `tasks-archive/2026-07/20260722-b2b2c-realm-and-app.md`.

## ops-app — first increment scope

- Backend: `OperatorUser` entity + repo (unique email) + `OperatorRealm` (`OrgPolicy.None`,
  `isSuperUser` operators) + module; wire into `server` kontainer + host routing (`ops.*`) + CORS
  (allow `localhost:36590`).
- Frontend: new `funktor-demo/ops-app` kraft SPA (clone adminapp wiring), port 36590, login via
  `operators` realm, `Apis` pointing at the ops API host, unified auth-state.
- Pages: org **list**, org **create/edit** form, **users-by-org** view, **dashboard** (counts:
  #orgs, #users, maybe recent signups).
- Needs a `users-by-org` API (new) + a simple stats API (new). Orgs CRUD reuses `OrgsApiFeature`.

## Open

- Host scheme for three apps (`ops.` / `b2b.` / `b2b2c.` subdomains) + the CORS/allowHost list.
- Whether the three apps share one server or split later.
- "users by org" — operators view b2b/b2b2c users of an org; needs cross-realm user lookup by org
  membership (design when we get there).
