# Auth + Organisations Foundation

**Status:** PLANNED 2026-07-17
**Goal:** Get organisations (tenants) and branches (sub-tenants) into users in the database, wire
the org-aware login flow (0/1/n org selection), and then fully implement all still-open user flows.
Auth is the center-piece for everything that follows.
**Supersedes:** Track B of `20260717-saas-foundation.md`. Absorbs the open Phases 2–6 of
`v1-email-auth-and-sessions.md` as Phase F.

## Decisions from Karsten (2026-07-17)

- `UserPermissions` is the intended structure: `organisations`/`branches` model the tenancy —
  organisation = chain, branch = site (hotel-chain model). "Not perfect but enough."
- Realm ≠ tenant, confirmed. Realms divide end-users vs. admin-users vs. global management
  interfaces.
- Unique key per email is OK; one account can be assigned to multiple organisations.
- **Single active org per session (Model C).** `UserPermissions.organisations: Set<String>` becomes
  `org: String?` (the one selected org). `branches`, `roles`, `permissions` are **driven from that
  org**, populated at login from what's stored in the DB. The flat sets — and every `has*` helper on
  them — stay exactly as they are; only the organisation-related helpers change.
- **No live org switching.** To act in a different org the user logs out and selects another org on
  the next login. No `switch-org` endpoint, no mutable session org, no per-org claim mapping.
- `accessibleOrgs: Set<String>` on `UserPermissions` lists every org the user may log into (for the
  0/1/n picker and the frontend's "log into another org" affordance). Non-authz-bearing.
- Login flow: **0 accessible orgs → no access; 1 → auto-select; n → intermediate selection step.**
- Per-org roles/permissions behave like **plan-driven feature switches** (populated from the org's
  purchased plan) — merged into the claims for the selected org on login and refresh.
- Target DBs are document stores only: **ArangoDB (karango) + MongoDB (monko)**. (Karsten must set
  both up locally before implementation can start — implementation is blocked on that.)
- **No backwards compatibility, no migrations.** Nothing relies on this yet — only the result counts.
- After the foundational structures: fully implement all open user flows. Everything else
  (data-layer scoping, document layer, starter, …) comes later.

### Why Model C (rejected alternatives)

- **Model A (Set of orgs + one selected):** splits state — the token holds the set, the frontend
  must track which is selected. Karsten flagged the awkwardness.
- **Model B (Map<org, access>, all orgs active in parallel, org context per request):** cleanest for
  true parallelism but spills into the whole authz layer — every `has*` and every `AuthRule` becomes
  org-aware. Rejected: too invasive, high blast radius.
- **Model C (single `org`, rest driven from it, re-login to switch):** keeps the flat permission
  model and its helpers untouched, sidesteps per-org claim mapping, removes the switch machinery.
  The only cost is "re-login to switch org", which is acceptable for what we're building.

---

## Target model (overview)

```
Organisation (vault entity, app-global)          User (app-owned entity, per realm)
 ├─ id, slug, name, status                        ├─ email (globally unique — OK)
 ├─ branches: List<Branch>  (embedded)            └─ memberships: Set<OrgMembership>
 │    └─ id, slug, name, status                        └─ orgId, branchIds, roles
 ├─ plan / feature switches                       Session (AuthRecord.Session)
 └─ attributes: TypedAttributes                    └─ + org: String?  (informational)

Realm = which user pool + interface              UserPermissions (in JWT, one selected org)
 ├─ orgPolicy: None      → admin/global mgmt       ├─ org            = the ONE selected org (or null)
 └─ orgPolicy: Required  → end-user realms         ├─ accessibleOrgs = all orgs user may log into
                                                   ├─ branches       ┐ driven from the selected org
                                                   ├─ roles          │ (membership + org plan),
                                                   └─ permissions    ┘ populated at login from DB
```

Semantics: `org` = the org this session is acting in; `accessibleOrgs` = where the user could log in
instead (switch = re-login); `branches`/`roles`/`permissions` = the selected org's slice, resolved
from the DB at login. `isSuperUser` stays the platform-wide bypass for admin realms.

## UserPermissions refactor (the crux)

```kotlin
// ultra/security/.../user/UserPermissions.kt  — breaking change, allowed (no back-compat)
@Serializable
data class UserPermissions(
    val isSuperUser: Boolean = false,
    val org: String? = null,                     // WAS: organisations: Set<String>
    val accessibleOrgs: Set<String> = emptySet(),// NEW: all orgs the user may log into
    val branches: Set<String> = emptySet(),      // unchanged — the selected org's branches
    val groups: Set<String> = emptySet(),        // unchanged
    val roles: Set<String> = emptySet(),         // unchanged — the selected org's roles
    val permissions: Set<String> = emptySet(),   // unchanged — selected org's perms/feature switches
)
```

- **Unchanged:** every `hasBranch/hasGroup/hasRole/hasPermission` family and `mergedWith` (the
  group/role/permission/branch parts). This is the whole point of Model C.
- **Rewritten (small, contained):** the organisation helpers, now single-valued —
  `hasOrganisation(x) = isSuperUser || org == x`; `canAccessOrg(x) = isSuperUser ||
  accessibleOrgs.contains(x)`. The `hasAny/hasAll Organisations` set-variants lose meaning with one
  org; drop them (grep confirmed no route uses them — the deep-dive found `forOrganisation` unused).
- **`org` nullable:** null for admin/`None` realms, anonymous, and system perms. `mergedWith` takes
  `org`/`accessibleOrgs` from `other` (like `isSuperUser`).

## How organisations relate to realms

- Organisations are **app-global entities** — one collection per app, realm-independent. Multiple
  realms can reference the same orgs.
- Each realm declares how it uses them via `orgPolicy`:
  - **`OrgPolicy.None`** (default): realm ignores orgs entirely. Admin/global-management realm
    (`AdminUserRealm` stays exactly as it is; `org = null`, super-users see across all orgs). The
    0/1/n logic never runs.
  - **`OrgPolicy.Required`**: end-user realm. Login runs the 0/1/n resolution; every issued session
    has exactly one selected org.
- **Single-tenant apps install a default org:** an `EnsuredOrganisation` config entry is upserted by
  an `OnAppStarting` lifecycle hook, and the realm's signup behavior auto-joins it. Login then always
  finds exactly 1 org → auto-select → users never see any org UI. Structurally every app is
  multi-tenant from day one; "single-tenant" is just an app whose realm auto-joins one ensured org.
  Flipping to real multi-tenancy later is config, not surgery.

---

## Design

### 1. New module `funktor/saas`

The SaaS umbrella module (Karsten, 2026-07-17): orgs/branches/membership live here now; future SaaS
machinery (invitations, plans/quotas, billing hooks, onboarding, per-org branding) joins without
another rename. Follows the house module pattern (commonMain models + jvmMain storage/api, Karango +
Monko repos, builder DSL). Module wiring: `Funktor_Saas` kontainer module + `FunktorSaasBuilder`.
Org-domain types inside stay org-named (`Organisation`, `OrgsStorage`, …). `funktor/auth` gains a
dependency on it. Alternative (fold into `funktor/auth`) rejected: rest/data layers will need org
context later without pulling auth.

```kotlin
// commonMain
@Serializable
data class OrgModel(val id: String, val slug: String, val name: String, val branches: List<BranchModel>)

// jvmMain — vault entity, both backends, unique index on slug
data class Organisation(
    val slug: Slug,
    val name: String,
    val status: Status,                    // Active / Suspended / Archived
    val branches: List<Branch> = emptyList(),  // EMBEDDED (SMB scale: tens of sites, not thousands)
    val plan: OrgPlan = OrgPlan.none,          // drives feature-switch permissions (see §5)
    val attributes: TypedAttributes = TypedAttributes.empty,
) {
    data class Branch(val id: String, val slug: Slug, val name: String, val status: Status,
                      val attributes: TypedAttributes = TypedAttributes.empty)
}
```

`OrgsStorage` (interface + Karango/Monko repos, realm-scoping index pattern from
`KarangoAuthRecordsRepo`), `FunktorSaasBuilder.useKarango()/useMonko()`, fixtures, and an
`OrgsApiFeature` with CRUD endpoints (`.authorize { isSuperUser() }` for now; org-admin rules later).

### 2. Membership on the user

User entities are app-owned (realms load/create them), so funktor provides the *contract* and the
value type; apps embed memberships on their user documents:

```kotlin
// funktor/saas commonMain
@Serializable
data class OrgMembership(
    val orgId: String,
    val branchIds: Set<String> = emptySet(),  // which sites of the org this user may access
    val roles: Set<String> = emptySet(),      // this user's roles within the org
)

interface HasOrgMemberships { val memberships: Set<OrgMembership> }
```

`AuthRealm` gains a hook with a working default:

```kotlin
suspend fun getMemberships(user: Stored<USER>): Set<OrgMembership> =
    (user.value as? HasOrgMemberships)?.memberships ?: emptySet()
```

Embedded-on-user (not a separate collection): matches Karsten's framing, one query at login,
array-indexable in both document DBs, SMB scale. Membership admin = normal app CRUD on its user
entity. Invitation flows come later.

### 3. Realm org policy + signup behavior

```kotlin
// funktor/auth
sealed interface OrgPolicy {
    data object None : OrgPolicy                                  // default — admin/global realms
    data class Required(val onSignup: SignupOrgBehavior = SignupOrgBehavior.None) : OrgPolicy
}

sealed interface SignupOrgBehavior {
    data object None : SignupOrgBehavior                          // invite-only products (later)
    data class AutoJoin(val orgSlug: String) : SignupOrgBehavior  // single/default-org apps
    data object CreateOwnOrg : SignupOrgBehavior                  // classic "create your workspace"
}

interface AuthRealm<USER> {
    val orgPolicy: OrgPolicy get() = OrgPolicy.None
    // ...
}
```

`CreateOwnOrg`: signup creates an org (name/slug derived from display name, app-customizable via
`createUserForSignup`) with the signing-up user as first member. Signup under `Required(None)`
succeeds but the user hits the 0-org gate at sign-in until a membership is assigned (correct for
invite-only; documented).

### 4. Org-aware sign-in: the 0/1/n flow

`AuthSignInResponse` becomes sealed (**breaking — allowed**); the current shape
(`model/AuthSignInResponse.kt:10-14`) becomes the `Success` variant:

```kotlin
@Serializable
sealed interface AuthSignInResponse {
    @Serializable @SerialName("success")
    data class Success(
        val token: Token,
        val realm: AuthRealmModel,
        val user: JsonObject,
        val org: OrgModel? = null,                // the selected org; null for OrgPolicy.None realms
    ) : AuthSignInResponse

    @Serializable @SerialName("org-selection-required")
    data class OrgSelectionRequired(
        val realm: AuthRealmModel,
        val selectionToken: String,               // short-lived, single-use (authenticated, not yet scoped)
        val organisations: List<OrgModel>,        // the user's accessible orgs, for the picker
    ) : AuthSignInResponse
}
```

Flow in `AuthRealm.signIn` (`AuthRealm.kt:191-204`), after `provider.signIn` returns the user:

- `OrgPolicy.None` → `Success(org = null)` as today.
- `Required`: resolve `getMemberships(user)` → accessible orgs (active only):
  - **0** → throw `AuthError.noOrganisationAccess` (credentials were valid; distinct from
    `invalidCredentials` on purpose — the user should see "no access", not "wrong password").
  - **1** → auto-select, issue `Success(org = it)`.
  - **n** → persist `AuthRecord.OrgSelectionToken` (new variant; realm, ownerId, token, `expiresAt`
    from new `RealmTokenConfig.orgSelectionTokenLifetime` default 5 min, single-use — same pattern as
    `PasswordRecoveryToken`), return `OrgSelectionRequired`.

New endpoint on `AuthApi`:

- `POST /auth/{realm}/select-org` — body `{ selectionToken, orgId }` → validates token (single-use,
  consume on success) + membership → `Success`.

**No `switch-org` endpoint** — to change org, the user logs out and logs back in, picking another
org. This removes mutable-session-org state entirely.

**Single token-issuance seam:** both issuance points (auto-select, select-org) go through one
internal `issueSignIn(user, selectedOrg)` helper. When Phase F wires session creation + the
`funktor:sid` claim into sign-in, only this helper changes — no rework of the org flow.

### 5. JWT + permissions building

- **No new namespaced claim.** `org` and `accessibleOrgs` are part of `UserPermissions`, so they
  travel in the existing permissions claim. `jwt/builder.kt` + `jwt/extract.kt` change:
  `organisations` array claim → `org` single-string claim + `accessibleOrgs` array claim. Localized.
- `generateJwt` signature (`AuthRealm.kt:166`) becomes `generateJwt(user, selectedOrg: SelectedOrg?)`
  where `SelectedOrg` carries the chosen org + the user's membership in it. Funktor provides the
  permissions builder so realms stop hand-rolling it — it **derives the selected org's slice from the
  DB**:

```kotlin
// selected = null for OrgPolicy.None realms
fun buildOrgPermissions(memberships: Set<OrgMembership>, selected: SelectedOrg?): UserPermissions =
    UserPermissions(
        org = selected?.org?.id,
        accessibleOrgs = memberships.map { it.orgId }.toSet(),
        branches = selected?.membership?.branchIds ?: emptySet(),
        roles = selected?.membership?.roles ?: emptySet(),
        // feature switches from the org's plan ∪ role-derived perms:
        permissions = selected?.let { it.org.plan.featurePermissions + resolve(it.membership.roles) } ?: emptySet(),
    )   // apps merge extras via mergedWith(...)
```

  Split of truth (design point — adjustable): **membership** owns the user's per-org roles + branch
  access; the **org's plan** owns the feature-switch permissions granted to everyone in the org. Both
  are merged into the flat claim sets for the selected org at login.
- `refreshToken` (`AuthRealm.kt:271`) re-issues for the **same** org (never changes it) and re-derives
  the slice from the DB (so role/plan changes take effect on refresh). Keeps the cross-realm `type` guard.
- `AuthRecord.Session` gains `org: String?` — informational (each session = one org's login; nice for
  the sessions UI). Not authz-bearing; authz reads `User.permissions.org`.

### 6. Active org context

No new provider needed. The active org is `user.permissions.org`, already available via the existing
`currentUserProvider()` (`funktor/rest/.../auth/call.kt:27`) since it's reconstructed from the JWT
permissions claim. AuthRule: `forOrganisation(id)` checks `permissions.org == id`;
`forSelectedOrganisation()` checks `permissions.org != null`. (This is the seam the later data-scoping
layer will read — see `20260717-saas-foundation.md`.)

### 7. Frontend (funktor/auth jsMain)

- `AuthState.signIn` handles the sealed response: `Success` → store token (as today);
  `OrgSelectionRequired` → expose state for the picker, then call `select-org`.
- `AuthFrontend` login page: org-selection view (list of `OrgModel` with name/branches, click →
  select-org → proceed).
- "Switch org" in an app = a **re-login** affordance: read `accessibleOrgs` from the decoded token to
  offer the choice, then log out → log in → pick. No live switch action.

---

## Phases

Each phase green + tested (both DB backends via the `MatrixTest2d`/`AppSpec` patterns) before the next.

### Phase O0 — UserPermissions refactor (~½ day) — do first, everything builds on it
- [ ] `organisations: Set<String>` → `org: String?` + add `accessibleOrgs: Set<String>`
- [ ] Rewrite organisation helpers (`hasOrganisation` single-valued, `canAccessOrg`); drop the
      set-variant org helpers; update `mergedWith`; leave branch/group/role/permission helpers untouched
- [ ] `jwt/builder.kt` + `jwt/extract.kt`: `org` string claim + `accessibleOrgs` array claim
- [ ] Update `UserPermissionsSpec` + JWT round-trip specs; fix the demo realm `generateJwt`
- [ ] Green: `:ultra:security:jvmTest` + auth JWT specs

### Phase O1 — `funktor/saas` module (~1 day)
- [ ] `Organisation` entity (embedded branches, plan) + `OrgModel`/`BranchModel` common models
- [ ] `OrgsStorage` + Karango/Monko repos (unique slug index) + fixtures + builder DSL + kontainer module
- [ ] `OrgsApiFeature` CRUD (`isSuperUser`), wired into `funktor/all`
- [ ] `EnsuredOrganisation` config + `OnAppStarting` upsert hook (single/default-org apps)
- [ ] Tests: repo CRUD both backends, ensured-org idempotency, slug uniqueness

### Phase O2 — Membership + permissions plumbing (~½ day)
- [ ] `OrgMembership` + `HasOrgMemberships` (commonMain)
- [ ] `AuthRealm.getMemberships` hook with `HasOrgMemberships` default
- [ ] `OrgPolicy` + `SignupOrgBehavior` on `AuthRealm` (default `None`)
- [ ] `buildOrgPermissions` helper (derives selected-org slice from membership + org plan)
- [ ] Tests: permissions builder (accessibleOrgs vs selected-org branches/roles/plan-perms), claim round-trip

### Phase O3 — Org-aware sign-in: 0/1/n (~1 day)
- [ ] `AuthSignInResponse` → sealed (`Success` / `OrgSelectionRequired`); adapt `AuthSignUpResponse`
- [ ] `AuthError.noOrganisationAccess`
- [ ] `AuthRecord.OrgSelectionToken` + `RealmTokenConfig.orgSelectionTokenLifetime` (5 min)
- [ ] 0/1/n resolution in `AuthRealm.signIn` behind the single `issueSignIn(user, selectedOrg)` seam
- [ ] `generateJwt(user, selectedOrg)` signature change; `refreshToken` re-derives same org; `Session.org`
- [ ] `select-org` endpoint; `forOrganisation`/`forSelectedOrganisation` rules read `permissions.org`
- [ ] Signup behaviors: `AutoJoin` + `CreateOwnOrg` wired through `createUserForSignup`
- [ ] Tests: 0→noOrganisationAccess; 1→auto-select; n→selection (token single-use, expiry, wrong-org
      rejection); refresh keeps same org; `OrgPolicy.None` realm regression (admin login byte-identical);
      SSO providers run the same 0/1/n path

### Phase O4 — Frontend + demo wiring (~1 day)
- [ ] `AuthState` sealed-response handling; org-selection view in `AuthFrontend`; accessibleOrgs read
- [ ] funktor-demo: end-user realm with `OrgPolicy.Required`; fixtures: 2 orgs ("chain" with
      branches + a plan), users with 0/1/n memberships; `AdminUserRealm` untouched (`None`)
- [ ] Manual click-through: all three login shapes + re-login-to-switch

### Phase F — Complete all open user flows (~4–5 days)
Execute `v1-email-auth-and-sessions.md` Phases 2–6 as specified there, org-aware on top:
- [ ] **F1** Session-auth middleware; sign-in creates `Session` rows (with `org`) via the
      `issueSignIn` seam; `touch()` wired (its Phase 2)
- [ ] **F2** Sign-up verification tokens + `AuthSystem.activate()` implemented + activation email
      (its Phases 3–4 backend; kills the `AuthRealm.kt:215` TODO)
- [ ] **F3** Email change flow (request/confirm) (its Phase 4)
- [ ] **F4** Session management API: list/revoke/logout; password-change revokes others; new-device
      email (its Phases 3+5)
- [ ] **F5** Demo pages for every flow end-to-end, including org selection (its Phase 6)
- [ ] **F6** Fix the `sendPasswordRecoveryEmil` typo (`AuthRealm.kt:47`) — no deprecation cycle
      needed (no back-compat), just rename

## Acceptance gate

A fresh demo run passes: sign-up (CreateOwnOrg) → verify email → login (1 org, auto-selected) → a
second fixture user with 2 orgs logs in via the picker → log out → log back in and pick the other org
→ change password (other sessions revoked) → logout. Admin realm login unchanged throughout.

## Confirmed decisions (2026-07-17)

1. **Branches embedded** in the Organisation document — document DBs only (arango/mongo), SMB scale,
   atomic updates; membership references `branchIds`. ✅
2. **Per-org roles/permissions as plan-driven feature switches** — stored in the DB (membership roles
   + org plan), merged into the selected org's claims on login and refresh. ✅
3. **New module `funktor/saas`** (SaaS umbrella, not `funktor/orgs`). ✅
4. **No live switching** — re-login to change org; no `switch-org` endpoint, no mutable session org. ✅
5. **No per-session branch selection** — the selected org's branches are authorization data; a
   "site picker" is app-level UI later. ✅
6. **Model C** — single `org` + `accessibleOrgs`, rest driven from the selected org. ✅

## Security-critical → red-team follow-up

This whole plan is security-critical (auth, tenancy, tokens). Per the dev workflow, each implemented
phase gets a `/feature-review` gate, and collect red-team tasks for a later pentest session:
cross-org data access / IDOR on org & branch ids, `OrgSelectionToken` replay or use across users,
0-org gate bypass, `refreshToken` changing the org, `isSuperUser` escalation from an end-user realm,
selection-token issued for realm A accepted by realm B.

## Out of scope now (tracked in `20260717-saas-foundation.md`)

Enforced org-scoped repositories (data layer), invitation flows, org-admin management UI,
per-org email branding, org lifecycle cascades (archive/delete), document layer, starter template.
