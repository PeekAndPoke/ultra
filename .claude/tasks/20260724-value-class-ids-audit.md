# Audit: value classes for ids & invariant values (auth / saas / b2b)

**Status:** FINDINGS COLLECTED 2026-07-24 — awaiting the user's decision to codify a STANDING RULE +
rollout scope. Do NOT migrate anything yet.
**Enabled by:** `20260724-slumber-value-class-support.md` (slumber now serializes value classes exactly
like kotlinx). **Blocked for STORED/queried ids by:** the Karango-KSP + Monko-KSP follow-up tasks
(`20260724-karango-ksp-value-class.md`, `20260724-monko-ksp-value-class.md`).
**Goal (user):** wrap ids and similar in value classes to prevent accidental assignment/comparison
(passing a `userId` where an `orgId` is expected — today both are `String` and compile fine).

## The hazard (concrete, in the current code)
Bare `String` identifiers are passed POSITIONALLY and are freely interchangeable:
- `AuthRecordStorage.findByToken(realm: String, type: String, token: String)` and
  `findAllByOwner(realm: String, type: String, owner: String)` — three same-typed args; a swap compiles.
- `AuthSystem.refreshTokenSession(realm: String, userId: String, expectedUserType: String?, currentOrgId: String?)`.
- `AuthSystem.selectOrg(realm, selectionToken, orgId)`, `SessionStore.listForUser(realm, ownerId)`.
Value classes (`RealmId`, `UserId`, `OrgId`, …) make each of these a distinct compile-time type.

## Candidates (tiered by value)

### Tier 1 — interchangeable ids (HIGHEST value; the swap hazard is real & pervasive)
- **`RealmId`** — `realm: String` across `AuthSystem`, `AuthRecordStorage`, `SessionStore`,
  `AuthApiClient`; demo `REALM` consts ("b2b"/"b2b2c"/"operators"/"admin-user"). FRAMEWORK-wide.
- **`UserId`** — `userId`/`ownerId: String` on `AuthRecord` (all subtypes), `SessionStore`,
  `OrgMembersStorage`, `AuthSystem`. NOTE: realm-qualified `_id` (`b2b_users/x`) — wrap the full string.
- **`OrgId`** — `orgId: String` on `AuthSelectOrgRequest`, `OrgMembership.orgId`, `UserPermissions.org`,
  `AuthSystem`. NOTE: the CONTRACT is the bare `_key` (not `_id`) — a single `OrgId` type does NOT by
  itself prevent `_key`/`_id` confusion (would need two types, likely over-engineering; keep the
  documented contract). Wrapping still kills the org-vs-user-vs-realm swap.

### Tier 2 — invariant-bearing values
- **`Email`** — the original driver; `email: String` on every user model/entity. Canonical lowercase.
  CAVEAT (see below): the value class gives TYPE safety; canonicalization must still happen at the
  write boundary — it is NOT enforced on deserialize.
- **`Slug`** — `Organisation.slug`, `Branch.slug`, `OrgModel.slug`. Subdomain-safe, `Slugs.normalize`.

### Tier 3 — scoped / structural (lower value, still useful)
- **`BranchId`** (`Branch.id`, `branchIds: Set<String>`), **`UserType`** (the `USER_TYPE` consts
  "B2bUser"/…), auth-record `type` and reset/selection **tokens** (transient — lower priority).

### Probably NOT
- **Role** (`roles: Set<String>`) — open-ended, app-defined strings; a value class adds friction to
  dynamic role handling. Structural roles already have `OrgRole` constants; leave roles as strings.

## Caveats the reviews surfaced (must inform the rule)
1. **Type-safety ≠ canonicalization.** slumber AND kotlinx construct a value class via its CTOR on
   deserialize, BYPASSING an `of()` factory. An `init {}` block can VALIDATE (throw) but cannot
   TRANSFORM (can't trim/lowercase). So `Email.of()` canonicalization does NOT survive deserialization
   of untrusted input — canonicalize/validate at the boundary (as `20260724-email-canonicalization.md`
   already does for add-member/login), and put any hard invariant in `init {}`. (Red-team item recorded
   in that task.)
2. **Stored/queried ids need KSP first.** A value-class FIELD serializes/reads through slumber already,
   but TYPE-SAFE QUERY PATHS (filtering by it in Karango/Monko) need the two KSP follow-up tasks. So
   Tier-1 stored ids (`OrgMember.userId`, `AuthRecord.ownerId`, `Organisation.slug`) depend on those.
3. **Blast radius.** These are FRAMEWORK types — a big, staged migration, not big-bang. The rule should
   apply to NEW code immediately and migrate existing types incrementally (per module, with tests).

## Proposed standing rule (DRAFT — for the user to codify)
- New code: wrap identifiers and handle-like values in a `@JvmInline value class` (`RealmId`, `UserId`,
  `OrgId`, `Email`, `Slug`, `BranchId`, …) instead of bare `String`.
- Hard invariants go in `init {}` (runs on ctor / deserialize); canonicalizing transforms happen at the
  boundary via a factory (`of(...)`), NOT relied on for deserialize.
- Naming: `<Thing>Id` for ids; the value name for values (`Email`, `Slug`).
- Migrate existing framework types incrementally, KSP-first for stored/queried ones.

## Next
- USER decides: adopt the rule? scope (new-code-only vs migrate)? where do the value types live
  (`ultra/security`? a new `ultra/ids`? per-module)? → then a migration plan + the KSP tasks land first.
