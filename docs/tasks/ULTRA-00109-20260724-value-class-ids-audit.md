# ULTRA-00109: Audit: value classes for ids & invariant values (auth / saas / b2b)

> **Status (2026-10-19): PAUSED.** FINDINGS ACTED ON; STANDING RULE STILL UNDECIDED (rechecked 2026-10-19). The migration this audit asked for ran —
> see ULTRA-00110 (value class ids migration): Tier 1 `RealmId`, `UserId`, `OrgId` and the Tier 2 email value (as
> `EmailAddress`) are DONE and in the tree; Tier 2 `Slug` and all of Tier 3 (`BranchId`, `UserType`) are not. What still waits on
> the maintainer is the thing this doc was written for — codifying the standing rule below (it is in neither `CLAUDE.md` nor
> `.claude/skills/code-style/`) and the scope for the rest. The earlier header's "Do NOT migrate anything yet" is void.

**Enabled by:** ULTRA-00105 (slumber value class support) (DONE — slumber serializes value classes exactly
like kotlinx). **Was blocked for STORED/queried ids by** the Karango-KSP + Monko-KSP follow-ups
(ULTRA-00107 (karango ksp value class), ULTRA-00108 (monko ksp value class)) — both DONE and archived; the migration doc
records that the "KSP-first" premise was falsified.
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
  `AuthSystem`. ✅ DONE 2026-07-25 (ULTRA-00115 (value class orgid)).
  ⚠️ **This entry's original NOTE was WRONG and has been superseded.** It said the contract was the
  bare `_key` and that a single `OrgId` type could not prevent `_key`/`_id` confusion. The user's
  2026-07-25 decision standardized the session org id on the FULL `collection/key` `_id`, and a single
  type DOES prevent the confusion — because `OrgId.init` REQUIRES that shape, so a bare `_key` cannot
  be constructed at all. Verified by mutation test: reverting the isolation guard to `._key` is caught
  by 7 tests. URL segments keep the bare key via `OrgId.key` (a url's collection comes from the route
  parameter's type). Wrapping also kills the org-vs-user-vs-realm swap, as originally noted.

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
   of untrusted input — canonicalize/validate at the boundary (as ULTRA-00106 (email canonicalization)
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

## History

- 2026-10-21: numbered ULTRA-00109 (was `docs/tasks/20260724-value-class-ids-audit.md`).
