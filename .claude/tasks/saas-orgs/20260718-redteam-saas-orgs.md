# Red-team: SaaS organisations (Phase O1)

**Status:** COLLECTED — do NOT execute during feature work; run in a dedicated pentest session
**Feature task:** `20260717-saas-organisation-storage.md`
**Scope:** `funktor/saas` — `Organisation`/`Branch` entity, `OrgsStorage`, `OrgsApiFeature`,
`ensureOrganisation` startup hook.

## Attack scenarios to attempt

### Access control on the CRUD API (`OrgsApi`, all `isSuperUser()`)
- [ ] Non-super-user JWT (regular member) calls list/get/create/update — must be denied, not just
      hidden. Confirm 401/403, no data leak in the error body.
- [ ] Anonymous calls to every endpoint — must be 401 (list+create confirmed in tests; verify get/update).
- [ ] API-key caller with crafted permissions — can it reach org CRUD?

### IDOR / id handling
- [ ] `GET/PUT /api/orgs/{id}` with a `_key` for an org in a different collection or a forged
      `collection/key` — does `findById`/`DOCUMENT(repo, id)` leak or cross repos? Test id values
      containing `/`, `..`, AQL/Mongo metacharacters.
- [ ] Enumerate orgs by guessing `_key`s (are keys sequential/predictable?).

### Tenant isolation (becomes critical once O2 membership scoping lands)
- [ ] After O2: a tenant admin of org A attempts read/update of org B via id — must fail.
- [ ] Branch-id spoofing: create/update an org with branch ids colliding with another org's.

### Data-integrity / storage
- [ ] Slug uniqueness under concurrency (cross-JVM) — two creates with the same slug; confirm the
      unique index rejects the loser and no partial/duplicate rows.
- [ ] `ensureBySlug` race: many concurrent callers (simulate multi-JVM boot) — exactly one org, no
      exception surfaced (the catch→re-read path holds).
- [ ] Oversized/hostile payloads: huge `branches` list, very long slug/name, unicode/control chars.

### Startup hook
- [ ] `ensureOrganisation` mis-config (empty slug, duplicate default across realms) — does boot fail
      safe or create garbage? Confirm an `AppStartException` path vs silent bad state.

### Org membership management (OrgMember collection — from the 2026-07-23 Increment-1a review)
- [ ] **Last-owner removal RACE**: two parallel requests (remove owner u1 + demote owner u2) both
      pass a check-then-act `wouldRemoveLastOwner` → org ends with ZERO owners (owner-locked, no
      principal passes `canManageOrgMembers`). Confirm the Increment-2 mutation path is atomic.
- [ ] **Cross-org MOVE via `save` identity mutation**: a change-roles endpoint that echoes a
      client body through `OrgMembersStorage.save` — try to reassign `OrgMember.org`/`userId` to
      another org/user (only the `(org,userId)` unique collision is caught by storage).
- [ ] **Ownerless-org lockout**: create an org (or accept an invite) without seeding an `OWNER`
      `OrgMember` → permanently unmanageable org.
- [ ] **Orphan-row enumeration**: delete a user, then confirm no stale `OrgMember` rows still surface
      in `findByOrg` (operator/admin member lists) or inflate seat counts (cascade not yet built).
- [ ] **Cross-org member enumeration/mutation**: as a tenant admin of org A, hit
      `/orgs/{B}/members[/{member}]` — the `OrgIsolationGuard` (via `OrgMember` being `OrgAware`)
      must 404 both the caller-binding and the member∈org check.
- [ ] **Role escalation via changeRoles**: a non-owner/admin (or a member of another org) attempts
      to grant themselves `OWNER`/`ADMIN`.
- [ ] **Stale token after removal**: a removed member keeps acting until their ~1h JWT expires
      (documented v1 behavior) — confirm no LONGER-lived acceptance.

## Notes
- Current CRUD is platform-super-user only; per-tenant authorization does not exist yet, so the
  cross-org isolation items are pre-registered for after O2/O3 add memberships + org-scoped rules.
- The OrgMember membership items are pre-registered for after Increment 2 (member-management API)
  lands; collected here, NOT executed during feature work.
