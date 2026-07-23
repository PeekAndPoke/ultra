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
- [x] **Migrate login** (2026-07-23): b2b/b2b2c realms override `getMemberships` →
      `OrgMembersStorage.sessionMembershipsOf(user._id)` (new reusable helper in `funktor/saas` maps
      `OrgMember.org._key` → session `OrgMembership.orgId`). Dropped the embedded `memberships` field
      from `B2bUser`/`B2b2cUser`; **removed `HasOrgMemberships`** (user decision: collection-only);
      default `getMemberships` → `emptySet()`. Fixtures seed `OrgMember` rows via `orgMembers.add`.
      Green: `B2bAuthFlowTest` 7 / `B2b2cAuthFlowTest` 5, `funktor:all` (both backends),
      `ultra:security` / `funktor:auth` / `funktor:saas`. Docs collector:
      `20260723-docs-membership-model.md`.

## Increment 2 (leaf) — member-management API + b2b Members page

- [ ] **Member API** `/orgs/{org}/members` (list) + `/orgs/{org}/members/{member}` (roles/remove),
      `OrgAwareParam`, owner/admin-gated (`authorize { forAny { hasRole(OWNER); hasRole(ADMIN) } }`),
      last-owner invariant enforced. Client + models. Lives in `funktor/saas` (core mechanism).
- [ ] **b2b-app Members page**: list the selected org's members + roles; change-role / remove.
- [ ] Document gap #4 (staleness: membership change bites on next ~1h token refresh) — accept for v1.
- [ ] e2e (both backends): list; foreign-org/non-member blocked (404 via guard); last-owner
      removal/demotion rejected; role change reflected; anonymous → 401 before load.

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
