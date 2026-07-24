# Org membership management + ownership (saas showcase — core function)

**Status:** IN PROGRESS — Inc 1a + 1b + 2a backend ALL DONE (review loops closed; 1a/1b committed,
2a about to commit 2026-07-24). Inc 2b (b2b-app Members page) is the next chunk.
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
- [x] **Migrate login** (2026-07-23): b2b/b2b2c realms override `getMemberships` →
      `OrgMembersStorage.sessionMembershipsOf(user._id)` (new reusable helper in `funktor/saas` maps
      `OrgMember.org._key` → session `OrgMembership.orgId`). Dropped the embedded `memberships` field
      from `B2bUser`/`B2b2cUser`; **removed `HasOrgMemberships`** (user decision: collection-only);
      default `getMemberships` → `emptySet()`. Fixtures seed `OrgMember` rows via `orgMembers.add`.
      Green: `B2bAuthFlowTest` 7 / `B2b2cAuthFlowTest` 5, `funktor:all` (both backends),
      `ultra:security` / `funktor:auth` / `funktor:saas`. Docs collector:
      `20260723-docs-membership-model.md`.

## Increment 2 (leaf) — member-management API + b2b Members page

- [x] **Member API** (2026-07-23) — `B2bMembersApi` in the demo b2b: `GET /api/b2b/orgs/{org}/members`
      + `PUT/DELETE .../{member}` (`{org}` OrgAwareParam, `{member}` `Stored<OrgMember>` OrgAware).
      Floor `forUserType(B2bUser)`; mutations `authorize { forAnyRole(OWNER, ADMIN) }`. Atomic
      last-owner via per-org `tryToLock`; `(org,userId)` immutable (change-roles copies only roles);
      `remove` uses the soft-delete path. List scoped to b2b members (resolve in `B2bUsersRepo`).
      Models + `B2bMembersApiClient` in `funktor-demo/common/b2b`. Wired via `B2bMembersApiFeature`
      + `B2bMembersServices` (dynamic).
- [ ] **b2b-app Members page**: list the selected org's members + roles; change-role / remove. (2b)
- [ ] Document gap #4 (staleness: membership change bites on next ~1h token refresh) — accept for v1.
- [x] e2e — `B2bMembersApiTest` (Karango, 4 tests): anonymous → 401; b2b-scoped list (b2b2c
      excluded); foreign-org → 404 (guard); sole-owner demote/remove rejected → 2nd owner unlocks →
      remove soft-deletes. Storage layer is both-backend via `OrgMembersStorage{Karango,Monko}Spec`.
      (Two-parallel-owner-removals race is a red-team scenario, not a deterministic e2e.)

### Increment 2a design (decided 2026-07-23) — b2b member API (demo-first)

- **Placement (user):** build in the demo b2b-app, composing core `OrgMembersStorage` + `OrgRole`
  invariants with `B2bUsersRepo` for display; extract the HTTP shape to `funktor/saas` later.
- **Endpoints** (URL-org, `OrgAwareParam`), group `B2bMembersApi(authFloor = { authenticated() })`:
  `GET /api/b2b/orgs/{org}/members` (list — any member; the guard binds the caller's session org),
  `PUT .../{member}/roles` + `DELETE .../{member}` (owner/admin via `authorize { forAny {
  forRole(OWNER); forRole(ADMIN) } }` — admits operators via the `isSuperUser` bypass).
  `{member}: Stored<OrgMember>` (OrgAware) → the part-3 guard checks member∈{org} for free.
- **Atomic last-owner** (the 1a security MEDIUM): per-org `GlobalLocksProvider.tryToLock(
  "b2b-org-members-<orgKey>")` around the check-then-act (owner set computed over ALL realms).
  `(org,userId)` immutable — changeRoles copies only `roles`. Lock-timeout → 409 retry; last-owner → 400.
- **v1 scoping:** the b2b member LIST shows only b2b-realm members (account admins) — rows whose
  `userId` resolves in `B2bUsersRepo`; b2b2c end-users of the same org are the operator surface's job.
  The last-owner INVARIANT still uses the full cross-realm owner set.
- **Soft-delete (user 2026-07-23):** removing a member SOFT-deletes (audit trail + recoverable),
  not a hard delete. No vault soft-delete query helper existed, so added a REUSABLE one to BOTH
  backends: `karango/core/.../aql/softdelete.kt` (`notDeleted`/`deleted` = `IS_NULL`/`IS_NOT_NULL`)
  and `monko/core/.../lang/dsl/softdelete.kt` (`Filters.eq/ne(path, null)`). `OrgMember` implements
  `SoftDeletable.Mutable`; `OrgMembersStorage.remove` soft-deletes (`withSoftDelete(SoftDelete(now))`);
  ALL reads (`findByUser`/`findByOrg`/`findByOrgAndUser`) exclude deleted via `notDeleted`. So a
  removed member vanishes from lists, the JWT, and the last-owner count. NOTE: the KSP types a nested
  `SoftDelete?` accessor as `AqlPropertyPath<SoftDelete, SoftDelete>` (non-null value) — the helper
  param is `AqlExpression<SoftDelete>` / `MongoPropertyPath<*, SoftDelete>`. Reactivation-on-re-add
  (a soft-deleted `(org,userId)` slot is retained → re-add collides) is deferred to the invite leaf.
  Green: OrgMembers Karango 7 / Monko 7; `funktor:all` + demo unaffected.
- **Foundation DONE (compiles):** `funktor-demo/common/.../b2b/` — `OrgMemberModel` +
  `ChangeMemberRolesRequest` + `B2bMembersApiClient`. **NEXT:** server `B2bMembersApi` (list /
  change-roles / remove with the atomic per-org lock, using the soft-delete `remove`) + feature +
  wiring + e2e (both backends) → review the whole 2a backend (member API + soft-delete + helpers) as
  one unit → then 2b (b2b-app Members page).

### Increment-2 constraints surfaced by the Increment-1a review (must honor)

- **Atomic last-owner enforcement (TOCTOU).** `wouldRemoveLastOwner` is a pure check; two concurrent
  owner-removals both pass it → zero owners (org owner-locked). The mutation API needs an ATOMIC
  guard (single-txn re-count-and-mutate / conditional write / per-org serialization), not
  check-then-act across two round-trips. Add a two-parallel-removals concurrency test. (security MEDIUM)
  Also key `ownerIdsOf`/`wouldRemoveLastOwner` on `OrgMember.userId` (realm-qualified), NOT
  `OrgMember._key` — both sides in the same identifier space or the sole-owner check mis-fires. (domain INFO r2)
- **`(org, userId)` is immutable at the mutation boundary.** `save()` updates by `_id` and would
  persist a changed `org`/`userId` — a change-roles endpoint echoing a client body could move a
  membership to another org/user. changeRoles must load the stored row and copy through ONLY
  `roles`/`branchIds`. (security LOW)
- **Seed an OWNER on org creation / invite-accept.** Nothing forces a new org to have an owner;
  `add(roles = emptySet())` is allowed. An ownerless org is permanently unmanageable
  (`canManageOrgMembers` false for all). (domain INFO)
- **Gate authorization via the AuthRule DSL, NOT the raw predicate (super-user bypass).** "May this
  caller manage members?" must go through `authorize { forAny { isSuperUser(); hasRole(OrgRole.OWNER);
  hasRole(OrgRole.ADMIN) } }` — `hasRole`/`hasOrganisation` (and the `OrgIsolationGuard` caller-binding)
  all start with `isSuperUser ||` (`UserPermissions.kt:54`), so an ORG-LESS super-user/operator
  (session `org == null`) passes. Do NOT gate by calling `permissions.roles.canManageOrgMembers`
  directly: the raw role-set predicates have NO super-user bypass and would wrongly DENY operators.
  When you do use the capability predicate (e.g. UI), use `canManageOrgMembers` not bare `isOrgAdmin`
  (owner-only ≠ `isOrgAdmin`). Conversely keep the last-owner INVARIANT (`wouldRemoveLastOwner`)
  NON-bypassing — a data-integrity rule that must hold for everyone, super-users/operators included.
  (surfaced by user Q 2026-07-23)
- **Translate the duplicate-`(org,userId)` DB exception** into a domain "already a member" result in
  the add/invite flow (cf. `OrgsStorage.ensureBySlug`). (impl INFO)
- **Cascade** `removeByUser`/`removeByOrg` must land before the operator-view / seat-limit leaves
  consume `findByOrg` (orphan rows otherwise surface + inflate seat counts). (domain LOW)
- **Per-realm-principal vs per-human**: `userId` is realm-qualified, so one human with accounts in
  two realms holds two rows in one org — seat counting / operator "users-by-org" must decide dedupe. (domain INFO)

## Test evidence

- [ ] Unit: `OrgRole` invariant helpers (sole-owner, last-owner-removal, promote/demote).
- [ ] e2e (both DB backends where storage touched): list members of my org (200); other-org /
      non-member caller blocked; last-owner removal/demotion rejected; role change reflected;
      anonymous → 401 before any load (reuse the no-read-when-anonymous pattern).
- [ ] Full backend suites green.

## ⏸ PAUSED 2026-07-23 — Increment 2a review round 1 DONE, decisions locked, fixes NOT yet applied

**State:** the 2a backend is BUILT + GREEN but UNCOMMITTED (`git diff d22c34f6`): soft-delete
helpers (karango/monko) + `OrgMember` soft-delete + `B2bMembersApi` (list/change-roles/remove) +
feature/wiring + `B2bMembersApiTest` (Karango 4/4) + `OrgMembersStorage{Karango,Monko}Spec` 7/7.
Round-1 3-agent review is COMPLETE (impl/domain/security). No CRITICAL/HIGH. Confirmed SAFE:
cross-org `{org}`/`{member}` both 404 via guard; `forUserType` denies b2b2c/operators at the floor
(no super-user bypass); `(org,userId)` un-smuggleable; zero-owner COUNT race prevented by the lock.

### Decisions (user, 2026-07-23) — apply in the round-1 fixes

- **Fork A — mutation scope = b2b-only (symmetric).** change-roles/remove must reject a target whose
  `userId` does NOT resolve in `B2bUsersRepo` → **404**, matching the list scoping. (b2b2c end-users =
  operator surface later.)
- **Fork B — owner-only ownership (standard SaaS model).** owner ⊃ admin. `forAnyRole(OWNER, ADMIN)`
  stays the member-management floor, but any op that ADDS or REMOVES ownership requires the CALLER to
  be an OWNER, else **403**. Ownership-touching = (`body.roles` grants OWNER) OR (the current target
  is an owner → demote/remove). Rationale: matches GitHub/Slack/Google Workspace/Stripe/Atlassian —
  least-surprising. (User asked for the known pattern; this is it.)

**RESUMED 2026-07-24 — fixes 1–7 APPLIED + green** (`B2bMembersApiTest` 6, `B2bAuthFlowTest` 7,
`OrgMembersStorage{Karango,Monko}Spec` 7). Details: reload-inside-lock via `resolveTarget`
(`findByOrgAndUser`, `notDeleted`) → 404 (MEDIUM); Fork A b2b-scope in `resolveTarget` → 404;
Fork B owner-only via `user.permissions.roles.isOrgOwner` → 403; dropped unused `deleted()` helper;
`services.x` direct (comment removed); e2e adds owner-only-403 + b2b2c-404 + mutate-removed-404;
red-team gets the resurrection race + admin-takeover. Added `owner@b2b.test` (OWNER) fixture.

**Round 2 (fresh 3-agent gate, 2026-07-24): domain PASS / impl PASS / security SAFE — no
CRITICAL/HIGH/MEDIUM; all round-1 fixes verified correct + non-vacuous.** Fixed: 1 LOW (class KDoc
misstated `list` gating — it's any-member, not owner/admin). Hardened: security INFO — `b2bUserOf`
verifies the full `_id` round-trips so a cross-realm bare-key collision can't pass the b2b scope
check. Noted: domain INFO-1 (demote is self-reversible in the ~1h window → docs "remove, don't
demote"; red-team scenario added). Recorded INFO (deferred): no role-catalog validation on
`body.roles` (backlog §4); `findByOrg` scan per mutation (accepted); b2b2c-owner anomaly
(operator-recoverable); admin can't edit an owner's app-roles (by design).

**Round 3 (fresh 3-agent gate, 2026-07-24): impl clean / domain PASS / security SAFE — ZERO code
findings. LOOP CLOSED.** Only item: a domain INFO refining the revocation KDoc (removal is not
"immediately effective" — both removal and demotion leave ~1h residual JWT power, gap #4; the real
difference is SELF-reversibility) — applied, doc-only. Security re-verified the `b2bUserOf` `_id`
round-trip closes the cross-realm bare-key collision on BOTH backends; all prior SAFE items hold.
**Increment 2a backend DONE.** Next: commit 2a → Increment 2b (b2b-app Members page).

### Fixes to apply BEFORE round 2 (all confirmed by ≥1 reviewer)

1. **[MEDIUM, all 3] Reload target INSIDE the lock.** In change-roles/remove, re-load via
   `orgMembers.findByOrgAndUser(params.org.asRef, params.member.value().userId)` (honors `notDeleted`)
   and operate on THAT fresh row; `null` → add `Outcome.NotFound` → **404**. Closes: the
   removed-member RESURRECTION race, the deterministic "mutate a soft-deleted member returns 200",
   and the audit-overwrite. (`B2bMembersApi.kt` changeRoles/remove.)
2. **[A] b2b-realm scope on mutations** — inside the lock, after reload, if
   `b2bUsers.findById(userId) == null` → 404.
3. **[B] owner-only ownership** — read the CALLER's roles via `RoutingContext.user.permissions.roles`
   (`funktor/core/.../core_module.kt:106` `RoutingContext.user`; `user.permissions`); if the op is
   ownership-touching and `!caller.roles.isOrgOwner` → **403**. (Import `isOrgOwner`.)
4. **[LOW] `deleted()` helper untested** — add a focused karango + monko unit test asserting
   `notDeleted` excludes / `deleted` includes a soft-deleted row on BOTH backends (it only rides the
   storage spec transitively today).
5. **[INFO] services accessor** — drop the misleading "resolved per request" comment / the `get()`
   indirection; use `services.x` directly like `FunktorConfApi`.
6. **e2e additions** — mutate-an-already-soft-deleted-member → 404; a b2b admin cannot mutate a
   b2b2c end-user of the org (404); an admin CANNOT grant OWNER / demote-remove an owner (403), an
   OWNER can. (`B2bMembersApiTest`.)
7. **red-team** (`20260718-redteam-saas-orgs.md`) — add: removed-member resurrection race;
   admin-takeover-via-OWNER-grant (now blocked by B).

### Recorded INFO / deferred (not round-1 blockers)

- Owner invariant is API-LOCAL (holds only because every owner-mutation takes the
  `b2b-org-members-<orgKey>` lock) — the operator/invite/cascade leaves MUST take the same lock or
  push enforcement into storage.
- `VaultGlobalLocksProvider` sanitizes the lock key (`[^a-zA-Z0-9]→"-"`) → two org keys differing
  only in non-alphanumerics share a lock (over-serialization, fail-safe; pre-existing).
- `loadMembers` is N+1 (`findById` per member) — fine at demo scale.
- 409 (lock-timeout) branch untested — acceptable with the deferred race.
- Removed member keeps access until their ~1h JWT expires — accepted v1 staleness (gap #4).

### Resume pointer

Apply fixes 1–7 → rebuild + green (`:funktor:saas:jvmTest`, `:funktor-demo:server:test`, the new
karango/monko helper tests) → **round 2** (fresh 3-agent gate on `git diff d22c34f6`) → loop to zero
→ COMMIT 2a → then **2b** (b2b-app Members page). Do NOT commit before the loop closes.

## Review record

### Increment 1a (storage foundation) — round 1 (3-agent gate, 2026-07-23)

| Reviewer | Verdict | In-diff findings (all FIXED) |
|---|---|---|
| 1. Impl & style | clean | MEDIUM `findByUser` full scan (no `userId` index) → added 2nd index both backends; MEDIUM `save()` untested → added modify test; LOW `Null.remove` silent noop → fail loud; LOW Null untested → `OrgMembersStorageNullSpec`; LOW `findByOrgAndUser` disambiguation unasserted → asserted |
| 2. Domain | PASS | LOW predicate bound to session type → extracted role-set core (`Set<String>.isOrgOwner/…`) reused by `OrgMember` |
| 3. Security | PASS | no injection / canonical `_id` carried / compound-unique real both backends / reads fail closed. Actionable findings are forward-looking → Increment-2 constraints |

- **Real bug caught by a review-fix test:** the disambiguation assertion exposed that Monko
  `findByOrgAndUser` used two separate `filter()` calls (the second overwrote the first) → matched by
  `userId` alone, returning the wrong org's row. Fixed to `filter(and(...))`. Karango was correct.
- **Dropped on verification:** R1 INFO "`repo.remove(member)` idiom" — `Repository` exposes only
  `remove(idOrKey: String)`; current `repo.remove(member._id)` is correct.
- **Forward-looking findings** (not in-diff defects) captured as Increment-2 constraints above +
  red-team scenarios below.
- Green after round-1 fixes: `OrgRoleSpec` 10; `OrgMembersStorage` Karango 6 / Monko 6 / Null 2.

### Round 2 (fresh 3-agent gate, 2026-07-23)

- Security **PASS (0)** — re-traced isolation/injection/canonical-`_id`/fail-closed; confirmed the
  Monko `and(...)` fix closes the wrong-row leak.
- Domain **PASS** — 1 forward-looking INFO (Inc-2 must key `ownerIdsOf`/`wouldRemoveLastOwner` on
  `OrgMember.userId`) → captured as an Increment-2 constraint.
- Impl — 1 LOW (`findByUser` scoping not pinned: a dropped `userId` filter would still pass the
  multi-user test) + 1 INFO (`remove` not proven targeted). **Both FIXED** — strengthened the
  multi-user test with `findByUser(u1) size 2` + `findByUser(u2)` = acme-only; `remove` now asserts
  the sibling row survives.
- **User-prompted hardening:** added `OrgIsolationSpec` test locking that a super-user is STILL
  subject to org-CONSISTENCY (foreign-org entity denied) — only the ACCESS gate has the super-user
  exception (`hasOrganisation` = `isSuperUser || …`; guard step 2 has no bypass). `OrgIsolationSpec`
  11→12. Increment-2 constraint added: gate authorization via the AuthRule DSL (super-user bypass),
  not the raw `canManageOrgMembers` predicate.
- Green: `OrgIsolationSpec` 12; `OrgMembersStorage` Karango 6 / Monko 6 / Null 2.

### Round 3 (fresh 3-agent gate, 2026-07-23) — LOOP CLOSED (zero findings)

- Impl **0**, Domain **0**, Security **0**. All three fresh reviewers traced the round-2/hardening
  test changes to the production code and confirmed each is NON-VACUOUS (the super-user-consistency
  test would fail under a stage-2 bypass; the `findByUser` pin catches a dropped userId filter; the
  `.toSet()` assertion alone would NOT — the added size + u2-only lines carry it). Core properties
  re-verified: no injection, canonical `_id`, compound-unique on both backends, fail-closed.
- **Increment 1a storage foundation is DONE** — review loop closed at round 3. Forward-looking
  findings recorded as Increment-2 constraints + red-team scenarios (not in-diff defects).

### Increment 1b (login migration) — round 1 (3-agent gate, 2026-07-23)

- Security **PASS (0)** — traced membership→JWT: cross-realm isolation intact (realm-qualified `_id`
  + distinct collection names), active-org VETTING preserved (stale/orphan rows neutralized at login),
  `_key` mapping byte-identical to the old embedded path, fail-closed defaults.
- Domain **PASS** — semantics preserved (0/1/n, active-only, suspended-exclusion all key on `orgId`,
  downstream vetting unchanged); `emptySet()` default is a behavioral no-op for org-less realms.
- Impl — 1 MEDIUM (roles/branchIds seam untested: a regression dropping them would keep all 12
  acceptance tests green). **FIXED** — `sessionMembershipsOf` both-backend test asserting
  `orgId=_key` + `roles` + `branchIds` round-trip + empty case.
- **Deferred follow-ups (recorded, not 1b blockers):**
  - **Double `getMemberships` fetch** (domain LOW): `issueSignIn`/`refresh`/`selectOrg` each fetch
    memberships, then `generateJwt` re-fetches — a pre-existing double-CALL that fork B turns into
    +1 `findByUser` query per auth op AND a divergence window (two independent reads can differ if a
    membership mutates between them). Cannot manifest in 1b (no runtime mutation until Inc-2; both
    reads vet active-only; recoverable at next ~1h refresh). Fix = thread the single resolved
    `Set<OrgMembership>` into `generateJwt` (a framework signature change) — closes BOTH angles; its
    own focused change, not a 1b bolt-on.
  - **Fixture cross-loader coupling** (domain INFO): demo user loaders write `OrgMember` rows into the
    (empty) OrgMembers loader's collection without a declared `dependsOn`. Safe under the current
    `installAllFixturesBeforeSpec` harness (no `removeAll`); latent hazard under a future
    `clearFixtures()`/selective install. Record; seed via the OrgMembers loader if it bites.
### Increment 1b — round 2 (fresh 3-agent gate, 2026-07-23) — LOOP CLOSED (zero findings)

- Impl **0**, Domain **0**, Security **0**. The `sessionMembershipsOf` seam test confirmed
  non-vacuous; migration re-verified clean (no dangling refs, DI wired, `_key` byte-identical,
  org-less `emptySet()` default a true no-op, cross-realm isolation intact, active-org vetting
  preserved, fail-closed). Both deferrals reconfirmed correctly scoped — no security dimension; the
  double-fetch consistency angle is latent-only until Inc-2 and closed by the same threading fix.
- **Increment 1b (login migration) is DONE** — review loop closed at round 2.

**Red-team follow-up:** extend `.claude/tasks/saas-orgs/20260718-redteam-saas-orgs.md` — last-owner
removal RACE (two parallel owner-removals → zero owners); cross-org MOVE via `save` identity mutation;
ownerless-org lockout (creation without owner seed); orphan `OrgMember` enumeration after user
deletion; ownership-predicate divergence if Inc-2 re-implements `isOwner` on `OrgMember`.

## Cross-references

- O2 membership/permissions: `20260718-o2-membership-permissions.md`.
- Org isolation (part 3/4): `.claude/tasks/auth-hardening/` — the isolation machinery this exercises.
- Backlog framing (core vs app, ownership §2, invites §3, roles §4): `20260719-saas-full-feature-backlog.md`.
