# Migration: wrap ids in `@JvmInline value class`es (RealmId pilot → the rest)

**Status:** PLANNED 2026-07-24 — awaiting go-ahead. `RealmId` is the pilot (Step 1) to surface
unforeseen complications before committing to the rest.
**Rationale / candidate survey:** `20260724-value-class-ids-audit.md`.
**Enabled by:** `20260724-slumber-value-class-support.md` (DONE — value classes serialize like kotlinx).
**User decision:** use SPECIFIC id classes (`RealmId`, `UserId`, `OrgId`, …), not a generic `Id<T>`.

## ✅ Prerequisite RESOLVED — and the "KSP-first" premise was falsified (verified 2026-07-24)
The plan assumed the Karango + Monko KSP had to gain value-class support before any stored+queried id
could migrate. **Verification-first probes proved that premise wrong:**

- **Neither KSP needs a change.** Both already render a value-class property with its value-class type
  (`append<RealmId, RealmId>("realm")`) — the type-safe accessor we want. (`entity.realm EQ RealmId(...)`
  type-checks; a bare `String` is rejected.)
- **Karango works fully as-is** — the `EQ` bind-var value is slumbered at execution, so `RealmId("b2b")`
  → `"b2b"` matches the stored scalar. Zero production change. (`20260724-karango-ksp-value-class.md`, DONE)
- **Monko needed only a small RUNTIME DSL fix** (filter values bypass slumber and the driver can't
  encode a value class): `unwrapValueClass` in `monko/core/lang/dsl/`, wired into every comparison
  operator. DONE. (`20260724-monko-ksp-value-class.md`)

**→ The stored+queried id migration is NO LONGER BLOCKED.** For `RealmId` concretely: `AuthRecord.realm`
(`field { realm }`, `FILTER(r.realm EQ realm)` / `r.realm eq realm`) will compile and query correctly
once the field type flips — Karango via slumber, Monko via the DSL unwrap. Both backends have
regression tests (codegen golden + Karango live round-trip; Monko unit at the driver-encoding boundary).
The live-Mongo round trip is the funktor/auth both-backend e2e that this very migration exercises.

## Placement (user 2026-07-24: co-locate with the entity's home WHERE POSSIBLE)
Rule: put each id in the SAME module as the entity it identifies; when a lower module also needs it
(dependency direction forbids co-location), fall back to the lowest module all users can see.
- `RealmId` → **`funktor/auth` commonMain** — co-located with the realm/`AuthRecord` concept (auth owns
  realms), in commonMain so BOTH the API client (commonMain) and `AuthRecord`/`AuthSystem` (jvmMain)
  can use it. Clean co-location. ✓
- `UserId` → `funktor/auth` commonMain — auth owns the realm-qualified user id.
- `OrgId` → CANNOT co-locate with `Organisation` (funktor/saas): `UserPermissions.org` /
  `OrgMembership.orgId` are in the LOWER `ultra/security`, which can't see funktor/saas. → lives in
  `ultra/security` (the lowest common module). Document the exception.
- `Email` / `Slug` → co-locate with the owning entity (`Slug` with `Organisation` in funktor/saas;
  `Email` with the user models). Confirm each at its step.

## Step 1 — `RealmId` (PILOT)
`@JvmInline value class RealmId(val value: String)` with `init { require(value.isNotBlank()) }` (a
realm key is a non-empty registered identifier; no canonicalization). NO stdlib concerns.

Migration surface (from the survey):
- **Thread `RealmId` through EVERYWHERE `realm: String` flows (user directive 2026-07-24).** Not just
  the entity — every signature. E.g. `AuthSystem.getRealm(realm: String): AuthRealm<*>` →
  `getRealm(realm: RealmId): AuthRealm<*>` **and ALL its siblings** (`signIn/signUp/selectOrg/…`),
  `AuthRealm`, `SessionStore.listForUser/revokeAllForUser(realm, …)`, `AuthApiClient(realm)`, demo
  `REALM` consts (`B2bRealm.REALM` etc. → `RealmId("b2b")`).
- **API boundary — generic value-class URI-param converter (user directive 2026-07-24).**
  `AuthApiFeature.RealmParam(val realm: String)` → `RealmParam(val realm: RealmId)`. The incoming
  param converters must convert a URI param `String` → the value class (and back for outgoing). Build
  **ONE generic value-class converter, both directions** (String↔value-class over a String backing),
  not a per-id converter. Find the converter registry (funktor/ktorfx REST param binding) and add the
  generic value-class case. This is the reusable seam that makes every subsequent id (`UserId`/`OrgId`/
  `Email`/`Slug`) work at the API boundary for free. NOTE: a converter constructs the value class from
  raw input → put hard invariants in `init {}` (runs on construction); the converter is the boundary
  where `of()`-style normalization belongs (see the cross-cutting caveat below).
- **Stored + queried (NOT blocked anymore — see the resolved prerequisite above):** `AuthRecord.realm`
  (all 6 subtypes) + the indexes/filters in `Karango/MonkoAuthRecordsRepo` (`FILTER(r.realm EQ realm)`
  / `r.realm eq realm`). Also `AuthRecordStorage` signatures (`findByToken(realm, type, token)`,
  `findAllByOwner(realm, type, owner)` — the swap-hazard functions this whole effort targets).

Test evidence:
- [x] Existing auth e2e green on BOTH backends (`B2bAuthFlowTest`, `B2b2cAuthFlowTest`, session/reset
      flows, `AuthApiSpec` HTTP flows) after the migration — behavior identical.
- [x] Both-DB round trip for `AuthRecord.realm` as `RealmId` (auth storage/session specs, both backends).
- [x] `RealmId` unit (`RealmIdSpec`): `init` rejects empty/blank/out-of-charset/over-length; equals &
      hashCode by value; serializes as a bare string. `ValueClassConverterSpec`: inbound/outbound +
      init-invariant→404 + `kotlin.*` exclusion.

**STATUS: Step 1 DONE 2026-07-25.** No KSP change needed (both KSPs already emit the type-safe
`append<RealmId, RealmId>` accessor). 3-agent gate PASSED — tenant isolation preserved, wire+storage
byte-identical (NOT a data migration), fail-closed converter. Hardening applied from review: `RealmId`
invariant tightened to `[A-Za-z0-9._-]`, 1..128 chars (a SECURITY boundary — makes the NUL-delimited
session-cache key collision-proof by construction); converter normalizes any ctor/init failure to
`IllegalArgumentException` (→ 404) and caches `ReifiedKType` per type.

**Converter boundary (reviewer-flagged, applies to all future ids):** the generic converter supports
value classes over a PRIMITIVE/enum backing only (delegates to `IncomingPrimitiveConverter`). Every
planned id (`RealmId`/`UserId`/`OrgId`/`Email`/`Slug`) is `String`-backed → fine; a value class over a
non-primitive (date/UUID/nested VC) would fail loudly at bind time — acceptable, documented.

## Step 2 — `UserId` ✅ DONE 2026-07-25
See `.claude/tasks/20260725-value-class-userid.md` (gate PASS). Two corrections to the plan below:
**placement is `ultra/security` commonMain, NOT `funktor/auth`** (`OrgMember` lives in funktor/saas,
which has no dependency on funktor:auth — ultra:security is the lowest common module and already owns
`UserRecord.userId`/`JwtUserData`/the CSRF key); and **the invariant is STRUCTURAL, not coll/key**,
because the same type carries synthetic non-document subjects (`anonymous`, `system`, `role-eval`,
API-key subjects) that are compared against real user ids. Wire+storage byte-identical.
Also landed as a by-product: request-body deserialization now maps a violated `init` to a **400**
instead of a 500 (`SlumberRestCodec.awakeBody`) — **Steps 3 and 4 depend on this**, since `orgId` and
the much stricter `Email` are both body fields.

### Original plan text
`userId`/`ownerId: String` → `UserId` (realm-qualified `_id`, e.g. `b2b_users/x`). Surface: `AuthRecord.
ownerId` (stored+queried → KSP), `SessionStore`, `OrgMembersStorage` (`OrgMember.userId` — stored+
queried → KSP), `AuthSystem`. Vault `Stored<T>._id` stays a generic `String`; `UserId` lives at the
auth/domain layer with conversion at the vault boundary (or a KSP helper). `init { require non-blank }`.

## Step 3 — `OrgId` ✅ DONE 2026-07-25
See `.claude/tasks/20260725-value-class-orgid.md` (gate PASS; atomic flip verified complete, and
mutation-tested — reverting the guard to `._key` is caught by 7 tests).

> ⚠️ **THE ORIGINAL PLAN TEXT BELOW WAS INVERTED AND IS NO LONGER THE CONTRACT.** It said
> `init { require(!value.contains("/")) }` — enforce the bare `_key`. The user's 2026-07-25 decision
> reversed this: **`OrgId` REQUIRES the full `collection/key` `_id`** and `init` enforces exactly that.
> Do NOT "restore" the old rule; doing so re-breaks every org-scoped request. Rationale: the
> project-wide rule is that anything naming another document names it by its globally resolvable
> `_id`, and the session org id was the last bare-`_key` holdout. Standardizing removes the
> "why is it a key here and an id there?" question rather than enshrining it.
>
> **Scope boundary (also the user's decision):** id-holding FIELDS are coll/key; **URL segments keep
> the bare `_key`**, projected via `OrgId.key`. A URL segment's collection comes from the route's
> parameter TYPE, and funktor's outgoing param converter renders every entity as `_key` — so this is a
> framework-wide convention, not an org exception. `OrgModel.id` stays a bare-`_key` String for that
> reason. (Putting coll/key in a path would mean `%2F` inside a path segment — routinely normalized or
> rejected by nginx/ALB/CDN — plus a UI reading "organisation/acme".)
>
> Not a DB migration (`UserPermissions`/`OrgMembership`/`SelectedOrg` are value objects, not `@Vault`
> entities; the persisted `OrgMember.org` `Ref` was already coll/key) but IS a wire/contract
> migration: in-flight JWTs carry the old bare key, so existing sessions must re-login.

### Original (superseded) plan text
`orgId: String` → `OrgId`. Surface: `UserPermissions.org`, `OrgMembership.orgId`, `AuthSelectOrgRequest.
orgId`, `AuthSystem.selectOrg`. ~~`init { require(!value.contains("/")) }` — enforces the bare-`_key`
contract~~ (INVERTED — see above). NOTE: `OrgMember.org` is a `Ref<Organisation>` (an `_id`), NOT an
`OrgId` — leave it; `OrgId` is the SELECTED-org id on the session/permissions side.

## Step 4 — `Email` ✅ DONE 2026-07-26 (as `EmailAddress`)
See `.claude/tasks/20260726-value-class-emailaddress.md`. Three corrections to the plan text below:

1. **Named `EmailAddress`**, not `Email` — `funktor/messaging` already owns `Email` (the MESSAGE), and
   `AuthRealm.DefaultMessaging` needs both.
2. **`init` enforces canonicality ONLY — not RFC format.** The plan's single `require(canonical)` was
   right and a format check was added, then REMOVED during review: SSO signup historically persisted
   whatever the provider returned, so a decode-time format invariant would render such a row
   permanently unreadable (user locked out; any list query containing it 500s). Format lives in `of` /
   `parseOrNull`, through which all external input passes — nothing invalid can enter, nothing stored
   becomes undecodable. Same "structural invariant" conclusion Step 2 reached for `UserId`.
3. `of()` also rejects non-ASCII raw input before lowercasing, so U+212A (which lowercases to `k`)
   cannot canonicalize onto a different existing address.

**Closes `.claude/tasks/20260724-email-canonicalization.md`** as its Option C — the 5 documented
lookup gaps are now fixed structurally.

### Original plan text
`email: String` → `Email`. `init { require(value == value.trim().lowercase()) }` + `of(raw) =
Email(raw.trim().lowercase())`. `of()` normalizes; a non-canonical value is REJECTED on construction
(incl. deserialize). CONSEQUENCE: `Email` is a DOMAIN/stored type — the API takes raw `String` and
normalizes at the handler (as `20260724-email-canonicalization.md` already does), NOT direct raw-body
binding. Surface: user models/entities across realms.

## Step 5 — `Slug`
`Organisation.slug` / `Branch.slug` → `Slug`. Same pattern as Email: `init` validates
`Slugs.normalize(value) == value`; `of(raw) = Slug(Slugs.normalize(raw))`.

## Later / optional
- `BranchId` (`Branch.id`, `branchIds: Set<String>`) — org-scoped; adopt when we touch that area.
- `UserType` (`USER_TYPE` consts) — closed set; value class OR keep consts; low payoff.

## Not
- `Role` (`roles: Set<String>`) — open-ended app-defined; leave as `String` (structural ones have
  `OrgRole` consts).

## Cross-cutting caveat (applies to every step)
Type-safety is guaranteed by the wrapper; CANONICALIZATION is NOT enforced on decode by construction
alone — slumber/kotlinx call the ctor, bypassing `of()`. Put hard invariants in `init {}` (runs on the
ctor / on deserialize → REJECTS bad values); do transforming normalization in `of()` at the boundary.
(Security red-team item recorded in the slumber task + email-canonicalization task.)

## Standing rule
To be codified by the user AFTER the RealmId pilot validates the approach (per the audit doc).
