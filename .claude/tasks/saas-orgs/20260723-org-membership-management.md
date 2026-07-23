# Org membership management + ownership (saas showcase — core function)

**Status:** IN PROGRESS (2026-07-23) — design decided, first slice starting.
**Plan:** `.claude/tasks/20260719-demo-restructure-three-apps.md` (the three-apps saas showcase is the
testbed) + `.claude/tasks/saas-orgs/20260719-saas-full-feature-backlog.md` §2 (ownership) / §3-§4.
**Security-critical:** YES — this is the org membership + permission spine (who belongs to an org, in
what role, and who may manage that). Red-team follow-up required at review (extend
`20260718-redteam-saas-orgs.md`).

## Why this is the "core" starting point (user directive 2026-07-23)

User: *"start from core functions and work yourself towards the leaves."* In the org-lifecycle tree,
**membership + ownership is the ROOT**: invitations create memberships, billing is owner-gated, the
operator surface views memberships. Everything else is a leaf hanging off this. So the showcase
continues here — and, per the user's framing, the value is as much in **surfacing framework gaps** as
in shipping the feature.

## Current model (as-is, established by O2 `20260718-o2-membership-permissions.md`)

- `OrgMembership(orgId, branchIds, roles)` — a `@Serializable` value object in **`ultra/security`**
  (`user/OrgMembership.kt`), embedded on the app user entity via `HasOrgMemberships.memberships`.
  Placed in `ultra/security` deliberately to avoid an `auth → saas` dependency inversion.
- Flow: `AuthRealm.getMemberships(user)` reads the user record → `resolveActiveSelectedOrg` /
  `buildVettedOrgPermissions` (`saas_org_hooks.kt`) → `buildOrgPermissions` → `UserPermissions` baked
  into the **JWT at login** (`B2bRealm.generateJwt`, 1h expiry).
- `Organisation` (saas) is the tenant root; `OrgsStorage` the global store. User stores are
  **per-realm** (`B2bUsersRepo` = Karango `b2b_users`, unique-email index, `findByEmail` only).
- Memberships are **fixture-seeded** today (`B2bUsersRepo.Fixtures`). No runtime management.
- **No ownership concept** — `"owner"`/`"admin"`/`"member"` are bare role strings; nothing enforces
  an invariant. (`getKnownRoles()` on `AuthRealm` is the role-catalog seam, per backlog §4.)

## Core modeling decision (DECIDED — fork B, user 2026-07-23)

The user first proposed elevating memberships onto the `AuthUser` interface (embedded), then chose
the cleaner separation: **a first-class `OrgMember` collection in `funktor/saas`, unique key
`(orgId, userId)`** (fork B). Embedded-on-`AuthUser` (fork A) was rejected.

- **Why B:** the leaves this feeds (operator "users-by-org" ACROSS realms, invitations with a status
  lifecycle, seat limits, billing seats) all want the org↔user relationship first-class and
  realm-agnostic — one indexed query lists an org's members for a tenant admin OR an operator, any
  realm. Embedded made "members of org X" a nested-array scan per realm store, unioned for operators.
- **`OrgMember` (saas entity):** `{ org: Ref<Organisation>, userId: String, roles: Set<String>,
  branchIds: Set<String> }`, Timestamped, and **`OrgAware`** (so a `/orgs/{org}/members/{member}`
  route gets BOTH part-3 guard checks free — caller-binding on `{org}` AND member∈{org}). `userId` is
  the realm-qualified user `_id` (globally unique across realm stores). `org: Ref<Organisation>`
  serializes to its `_id` string (verified in vault `RefCodec` — single ref → string, `Set`/`List`
  → collection of strings), so it stores/indexes/filters as a string; the guard reads `org._id`
  without resolving.
- **Layering keeps auth independent of saas:** the SESSION model is unchanged — `getMemberships()`
  stays THE seam; the org realms OVERRIDE it to query `OrgMembersStorage.findByUser(user._id)` and
  map each `OrgMember` → the `ultra/security.OrgMembership` session value object
  (`orgId = member.org._key`). The JWT still carries the selected org's roles exactly as today. Auth
  never references saas; the demo realm (which already depends on both) wires the override.
- **Ownership** = reserved structural role (`OrgRole.OWNER`/`ADMIN`) inside the row's `roles`, with
  the invariant enforced at the mutation boundary (Increment 2). Primitives already landed (`6aa49132`).
- **Cost accepted:** bigger refactor of the (just-hardened) login path + referential integrity —
  cascade-remove `OrgMember` rows when a user or org is deleted (embedded got this for free).

## Framework gaps this slice surfaces (the point of the exercise — verify/triage each)

1. **No query-by-org-membership seam.** User stores index by email only; listing an org's members
   needs `findMembersOfOrg(orgId)`. Where does it live — `AuthUserAdapter`? a new adapter method?
2. **No membership-mutation seam.** No framework path to add/remove/change a user's memberships;
   today you'd hand-mutate `user.memberships` and `save`. Needs a safe, invariant-checked primitive.
3. **No ownership primitive / invariant.** `"owner"` is a bare string. Need `OrgRole` constants +
   invariant helpers (`owners()`, `wouldRemoveLastOwner()`), owner/admin-gated management.
4. **Session staleness on membership change.** Memberships are baked into the JWT at login; a role
   change / removal only bites on next refresh (~1h) — same "next refresh" semantics as org
   suspension (`saas_org_hooks.kt`). No revocation/re-issue. Decide: acceptable-for-v1 + documented,
   vs a refresh/revocation seam.
5. **Session-org scoping vs URL-param scoping.** Part-3 org isolation scopes via URL `OrgAwareParam`.
   A "manage MY org's members" route scopes to the session's **selected org** (`UserPermissions.org`),
   for which there is no framework helper today. Possible new primitive: a current-org accessor/guard.
6. **Cross-realm membership** (operator view) — an org's members span realms; no framework support.
   Deferred to the operator leaf; recorded here.

## Route-scoping decision (user 2026-07-23): URL-org / OrgAwareParam

Tenant self-service routes carry the org in the URL (`/orgs/{org}/members/...`), params implement
`OrgAwareParam` → the hardened part-3 `OrgIsolationGuard` caller-binds `{org}` to the session org.
The operator surface (cross-tenant) later reuses the SAME pattern (super-user passes any org). Not
session-org-implicit. With `OrgMember` being `OrgAware`, the guard also auto-checks member∈{org}.

## Increment 1 (core spine) — introduce OrgMember + migrate login off embedded memberships

- [x] **Ownership primitives** (`ultra/security`): `OrgRole` (`OWNER`/`ADMIN`) + `isOwner`/`isAdmin`/
      `canManageMembers` + `ownerIdsOf`/`wouldRemoveLastOwner`. 9 unit tests. (`6aa49132`)
- [x] **`OrgMember` entity + `OrgMembersStorage`** in `funktor/saas` (Null + Vault + Repo; Karango +
      Monko), unique `(org, userId)` index; queries `findByUser`, `findByOrgAndUser`, `findByOrg`;
      `add`/`save`/`remove`. Registered in the module (default Null; `useKarango`/`useMonko` swap in
      Vault + repo + Fixtures). Both-backend base spec `OrgMembersStorage{Karango,Monko}Spec` — 5
      tests each, green. (2026-07-23) NOTE: a `Ref<Organisation>` field is modelled by KSP as an
      `AqlExpression<String>` (its `_id`), so filter on `org._id` — confirms refs serialize to
      strings. Cascade `removeByUser`/`removeByOrg` deferred until org/user deletion is wired.
- [ ] **Migrate login**: b2b/b2b2c realms override `getMemberships` → query `OrgMembersStorage`;
      drop the embedded `memberships` field from `B2bUser`/`B2b2cUser`; remove `HasOrgMemberships`
      (fold intent into the seam; default `getMemberships` → `emptySet()`); seed `OrgMember` rows in
      fixtures. Keep `B2bAuthFlowTest`/`B2b2cAuthFlowTest`/`OrgIsolationE2eSpec` green (both backends).

## Increment 2 (leaf) — member-management API + b2b Members page

- [ ] **Member API** `/orgs/{org}/members` (list) + `/orgs/{org}/members/{member}` (roles/remove),
      `OrgAwareParam`, owner/admin-gated (`authorize { forAny { hasRole(OWNER); hasRole(ADMIN) } }`),
      last-owner invariant enforced. Client + models. Lives in `funktor/saas` (core mechanism).
- [ ] **b2b-app Members page**: list the selected org's members + roles; change-role / remove.
- [ ] Document gap #4 (staleness: membership change bites on next ~1h token refresh) — accept for v1.
- [ ] e2e (both backends): list; foreign-org/non-member blocked (404 via guard); last-owner
      removal/demotion rejected; role change reflected; anonymous → 401 before load.

## Test evidence

- [ ] Unit: `OrgRole` invariant helpers (sole-owner, last-owner-removal, promote/demote).
- [ ] e2e (both DB backends where storage touched): list members of my org (200); other-org /
      non-member caller blocked; last-owner removal/demotion rejected; role change reflected;
      anonymous → 401 before any load (reuse the no-read-when-anonymous pattern).
- [ ] Full backend suites green.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up:** extend `.claude/tasks/saas-orgs/20260718-redteam-saas-orgs.md` (cross-org
member enumeration/mutation, last-owner-removal bypass, role-escalation via changeRoles, stale-token
after removal).

## Cross-references

- O2 membership/permissions: `20260718-o2-membership-permissions.md`.
- Org isolation (part 3/4): `.claude/tasks/auth-hardening/` — the isolation machinery this exercises.
- Backlog framing (core vs app, ownership §2, invites §3, roles §4): `20260719-saas-full-feature-backlog.md`.
