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

## Core modeling decision (DECIDED — fork A)

**Membership stays ON the user record** (fork A), NOT a new first-class saas entity (fork B).
- Rationale: consistent with the deliberate O2 architecture (membership is a claim on identity; the
  realm owns the user store) and the "realm = identity boundary" decision
  (`20260719-demo-restructure-three-apps.md`). Smaller, reversible (extract an entity later iff
  cross-realm querying actually hurts — YAGNI). Fork B would re-plumb the just-hardened
  session/isolation path and invert the dep O2 avoided.
- Consequence to accept + document: **membership is realm-partitioned** (each realm's user store);
  a member-mgmt mechanism operating on the caller's own realm is fine, but a cross-realm "all members
  of org X" (operator view) needs a store-union — deferred to the operator leaf.
- **Ownership** = a reserved structural role (`OrgRole.OWNER`, plus `OrgRole.ADMIN`) inside
  `OrgMembership.roles`, with the invariant enforced at the mutation boundary.

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

## First slice (Increment 1) — scope

Smallest coherent core slice that exercises the isolation/two-phase machinery on a real app route:

- [ ] **Ownership primitives** (`ultra/security`, fork-independent): `OrgRole` reserved constants
      (`OWNER`, `ADMIN`) + pure invariant helpers over a member set (`owners`, `isSoleOwner`,
      `wouldRemoveLastOwner`). Unit-tested.
- [ ] **Member query seam**: a way to list the members of an org from the caller's realm user store
      (`B2bUsersRepo.findMembersOfOrg(orgId)` Karango query + a framework seam it satisfies).
- [ ] **Member-management API** scoped to the caller's **selected org** (owner/admin-gated,
      invariant-enforced): `listMembers`, `changeRoles`, `removeMember`. Answers gap #5 (how to scope
      to the session org). First cut may live in the b2b demo with reusable bits noted for a
      `funktor/saas` extraction (the demo-restructure "streamlining goal").
- [ ] **b2b-app Members page**: list the selected org's members + roles; change-role / remove
      (owner/admin only).
- [ ] Document gap #4 (staleness) — likely accept + document for v1.

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
