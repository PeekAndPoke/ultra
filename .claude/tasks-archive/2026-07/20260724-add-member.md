# Add member to an organisation (b2b leaf) + reactivate soft-deleted slot

**Status:** DONE — 2026-07-24. Review loop closed at round 1 (zero production-code defects; the
follow-ups were doc/test-only, applied). The next leaf off the b2b membership arc
(`20260723-org-membership-management.md`): closes the member-CRUD story with a runtime ADD path
(memberships were fixture-only).
**Plan:** `.claude/tasks/20260719-demo-restructure-three-apps.md` (three-apps showcase) +
`20260719-saas-full-feature-backlog.md` §3 (invites).
**Security-critical:** YES — a new membership-mutation surface (who can add whom, with what roles).
Extend `20260718-redteam-saas-orgs.md` at review.

## Governing principles (user, 2026-07-24) — see memory `layer-placement-nearest-leaf`
- **Nearest-to-leaf placement.** Member administration is pure b2b → the ADD endpoint + UI live in the
  b2b demo app. Only genuinely cross-cutting primitives go in framework, and only after real
  repetition ("fool me once/twice" — abstract on observed duplication, never on prediction).
- **Applied here:** the reactivation *policy* (insert / reactivate / already-a-member) lives in the
  b2b leaf. The framework gains ONLY a generic missing read — `findByOrgAndUserIncludingDeleted` —
  which any reactivation/audit path (any app) needs and which the leaf cannot do without. If b2b2c/ops
  later need the same add orchestration, THAT is when we promote it to `OrgMembersStorage`.

## Scope (minimal, coherent)
Add an EXISTING b2b user to the caller's org by email, with roles.

- **Endpoint** (b2b leaf): `POST /api/b2b/orgs/{org}/members` (`{org}` `OrgAwareParam`).
  Floor `forUserType(B2bUser)`; `authorize { forAnyRole(OWNER, ADMIN) }`. Body `AddMemberRequest(email,
  roles)`. Per-org lock (`b2b-org-members-<orgKey>`, same as the other mutations) around the
  check-then-write. Orchestration inside the lock:
  - resolve the email → a b2b user (`B2bUsersRepo.findByEmail`); not found → **404** (b2b-scoped, like
    the rest of the surface — we only add b2b-realm users).
  - `orgMembers.findByOrgAndUserIncludingDeleted(org, userId)`:
    - active row (softDelete == null) → **409** "already a member".
    - soft-deleted row → **reactivate**: `save(row.copy(softDelete = null, roles = <request>, ...))`.
    - none → `orgMembers.add(org, userId, roles)`.
  - **owner-only ownership**: if the request grants OWNER and the caller is not an owner → **403**
    (mirrors `changeRoles`). Adding a non-owner role is fine for owner/admin.
  - returns the resulting `OrgMemberModel` (200).
- **Framework** (`funktor/saas`): `OrgMembersStorage.findByOrgAndUserIncludingDeleted(org, userId)` +
  `Repo` + Karango + Monko (the SAME query as `findByOrgAndUser` but WITHOUT the `notDeleted` filter).
  Null → returns null.
- **Client/models** (`funktor-demo/common/b2b`): `AddMemberRequest(email: String, roles: Set<String>)`;
  `B2bMembersApiClient.Add` (Post base) + `add(org, request)`.
- **UI** (`funktor-demo/b2b-app`): an "Add member" button on `MembersPage` opening a modal (email
  input + role checkboxes, owner locked unless caller is owner), reusing the modal/toast/reload idiom.

## Deferred (NOT this increment — build only when the demo calls for it)
- Full **Invitation lifecycle**: a pending `Invitation` entity, tokens, email, accept flow, and
  **new-user** invites (invitee has no account yet). This increment only adds EXISTING b2b users.
- **Role-catalog validation** on the request roles (`getKnownRoles`, backlog §4) — roles stay opaque.
- **Seed-owner-on-org-create** (the ownerless-org gap) — an ops/framework concern, tracked separately.
- Promoting the reactivation orchestration to `OrgMembersStorage` — only if a 2nd app needs it.

## Test evidence
- [x] Storage (both backends): `findByOrgAndUserIncludingDeleted` returns an active row, returns a
      soft-deleted row (where `findByOrgAndUser` returns null), returns null when absent.
      (`OrgMembersStorage{Karango,Monko}Spec` 8/8.)
- [x] e2e (Karango): add a new member (200); add an existing active member → 409; add a
      previously-removed member → reactivates (200, roles applied); admin granting OWNER on add → 403;
      unknown email → 404; anonymous → 401; foreign org → 404. (`B2bMembersApiTest` 11.)
- [x] Full backend suites + `:funktor-demo:b2b-app:compileKotlinJs` green.

## Review record — 3-agent gate (2026-07-24) — LOOP CLOSED (round 1)

All three reviewers found the production code correct — **zero CRITICAL/HIGH/MEDIUM, zero
production-code defects**. Every finding was doc/test/info; all applied (so no full round-2 needed for
doc/test-only follow-ups).

- **Impl+style: PASS.** Reactivation orchestration, both-backend framework read, the shared `roleField`
  refactor (no double-toggle; owner-lock intact in both modals), and the e2e/storage tests all verified
  correct. 1 LOW (test robustness): the add block left `noorg@` active → coupled to the `list`
  exact-membership test's order. FIXED — the block now soft-deletes `noorg@` at the end (order-neutral).
- **Domain: PASS.** Status codes (404/409/403), reactivation, and invariant handling correct and
  consistent with the locked decisions. Verified: add never touches the last-owner invariant; an admin
  **cannot launder ownership** via remove→re-add (removing/adding an owner both require owner). 2
  doc-only items FIXED: (1) `addMember` KDoc now notes reactivation is a destructive overwrite of the
  removal record + keeps original `createdAt`; (2) `OrgMembersStorage.add()` KDoc now warns it collides
  on a retained soft-deleted slot and points to the `findByOrgAndUserIncludingDeleted` + `save()`
  reactivation pattern (keeps the policy in the leaf per the placement principle, documents the hazard).
- **Security: no exploitable findings.** Cross-org add blocked by the shared `OrgParam` guard; owner-only
  enforced server-side before the existing-check; reactivation scoped by org+userId with `(org,userId)`
  immutable; all writes under the per-org lock; `findByEmail` parameterized; email echoed only in an
  escaped toast. FIXED: added an add-specific cross-org e2e (`single@`→globex → 404). LOW/INFO →
  red-team collector (`20260718-redteam-saas-orgs.md`): existence-enumeration oracle, reactivation
  hijack, ownership laundering, add-role injection, unsolicited membership, add TOCTOU. Deferred
  (pre-existing/backlog): server-side role allowlist (§4), invite/accept consent flow.

## Deferred follow-ups confirmed (not this increment)
- Invite/accept CONSENT flow + new-user (no account yet) invites; server-side role allowlist
  (`getKnownRoles`, backlog §4); append-only membership-event log if audit/tenure matters; promoting the
  reactivation orchestration to `OrgMembersStorage` only if a 2nd app (b2b2c/ops) needs it.
