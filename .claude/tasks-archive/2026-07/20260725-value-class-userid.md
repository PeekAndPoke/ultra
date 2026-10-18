# `UserId` value class (value-class-ids Step 2)

**Status:** DONE 2026-07-25 (gate PASS — all three reviewers, confirmed findings fixed, tests green)
**Plan:** `.claude/tasks/20260724-value-class-ids-migration.md` → Step 2
**Security-critical:** yes (identity field; feeds session lookup, ownership checks, CSRF hash key)



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `14753aba` | 2026-07-25 | 74 | feat(security): thread UserId value class end-to-end + fail-closed JWT identity |

Archived by `0f29d239` (rename only — that commit's code belongs to another task).

### Files changed (74)

Listed by module — full paths via the command below.

- **funktor/auth** — 18 files
- **ultra/security** — 15 files
- **funktor/rest** — 12 files
- **funktor-demo/server** — 10 files
- **funktor/saas** — 7 files
- **funktor-demo/common** — 6 files
- **funktor/all** — 4 files
- **funktor-demo/b2b-app** — 1 files
- **funktor/insights** — 1 files

```
git show --stat 14753aba
```

## Spec

Wrap the user-identity `String` in `@JvmInline value class UserId`, threaded through every signature
that carries it (per the standing user directive from the `RealmId` pilot: thread it EVERYWHERE, not
just the entity).

- [x] `UserId` in `ultra/security` commonMain (`user/UserId.kt`)
- [x] `UserRecord.userId` + the `Anonymous`/`System` sentinels + `isAnonymous()`/`isSystem()`
- [x] `JwtUserData.id` (the JWT subject claim)
- [x] `AuthRecord.ownerId` (interface + all 6 subtypes) — stored + queried
- [x] `AuthRecordStorage` (`owner` params on `findLatest`/`findAllByOwner`/`removeAllByOwner`/…)
- [x] `SessionStore` (all impls: `Vault`, `Cached`, `Null`) — `ownerId` params
- [x] `AuthRealm` / `AuthSystem` / `EmailAndPasswordAuth` signatures
- [x] Karango + Monko auth-records repos (indexes + filters)
- [x] `OrgMember.userId` + `OrgMembersStorage` + both saas repos + `OrgMemberships`
- [x] `AuthSetPasswordRequest.userId`, `AuthLoginApi` ownership check
- [x] `funktor/rest`: `Caller.userId`, `UserRecord.ApiKey`, the `role-eval` probe
- [x] demo: repos, `B2bMembersApi`, `OrgMemberModel.userId`, b2b-app pages
- [x] the four demo user models (`B2bUserModel`/`B2b2cUserModel`/`AdminUserModel`/`OperatorUserModel`)
      `.id` — `B2bUserModel.id` is compared against `OrgMemberModel.userId` in `MembersPage`, so it
      must share the type; all four are `id = _id` (the realm-qualified `_id`)
- [x] wire + storage format byte-identical (NOT a data migration)

## Implementation notes

**Placement: `ultra/security` commonMain — NOT `funktor/auth`** (the migration plan said funktor/auth;
that was wrong). `OrgMember` lives in `funktor/saas`, which has NO dependency on `funktor:auth` (they
are siblings). The lowest common module both `funktor/auth` and `funktor/saas` (via `funktor:core`)
can see is `ultra:security` — which already owns the user-id concept (`UserRecord.userId`,
`JwtUserData`, `OrgRole.ownerIdsOf`, the CSRF hash key). Contrast `RealmId`, which could live in
`funktor/auth` only because "realm" is auth-exclusive.

**The invariant is structural (non-blank, bounded, no control chars) — it does NOT require the
`collection/key` shape.** Verified reason: `UserRecord.userId` is a SUBJECT/identity field, not
purely a document pointer. It also carries synthetic actors that are not documents:
`UserRecord.ANONYMOUS_ID` / `SYSTEM_ID`, the `role-eval` probe (`funktor/rest/.../ApiRoute.kt:74`),
and opaque API-key subjects (`funktor/rest/.../auth/call.kt:42`). Those are compared against real
user ids (`funktor/auth/.../api/AuthApi.kt:208`), so they must share one type.

**The coll/key convention still holds where it applies:** every PERSISTED user reference already
stores the full realm-qualified `_id` — `ownerId = user._id` at every creation site, and
`OrgMember.userId` is documented as "the realm-qualified user `_id`" (proven by
`B2bMembersApiTest.kt:64` asserting `.userId.startsWith("b2b2c_users/")`). No format change here.

## Two behaviour changes worth reviewer attention

1. **`Payload.extractUser` is now fail-closed** (`ultra/security/.../jwt/extract.kt`). Previously a
   token with no id claim and no `sub` produced `UserRecord.LoggedIn(userId = "")` — a nameless yet
   AUTHENTICATED user. `UserId("")` would throw, which would turn a malformed token into a 500, so
   the resolution is: any missing/blank/structurally-invalid id degrades to `ANONYMOUS_ID`. Strictly
   safer than before. Same treatment on the JS side (`AuthState.readJwt` → `tokenUserId: UserId?`).
2. **`ChangePasswordWidget`** now shows `State.Error` when the session token carries no usable user
   id (previously it posted `userId = ""`).

**Verified NOT affected:** the CSRF hash key (`StatelessCsrfProtection.sign` interpolates `$userId`,
and `UserId.toString()` returns the bare value → byte-identical input; `UserId` also forbids control
characters, so the NUL delimiter stays collision-proof by construction) and the JWT `sub`/`user/id`
claims (written via `.value`).

## Test evidence

- [x] `UserIdSpec` — invariant (blank / over-length / control chars), accepts the `coll/key` form AND
      the synthetic subjects, equality by value, bare `toString`, inline-string serial descriptor,
      and `coll/key != bare key` (pins the convention)
- [x] Existing auth e2e green on BOTH backends (auth flows, session/reset, `AuthApiSpec`)
- [x] Both-DB round trip for `AuthRecord.ownerId` and `OrgMember.userId` as `UserId`
      (`OrgMembersStorage{Karango,Monko}Spec`, auth storage/session specs)
- [x] Full commands run + green: `:ultra:security:jvmTest`, `:funktor:rest:jvmTest`,
      `:funktor:auth:jvmTest`, `:funktor:saas:jvmTest`, `:funktor:all:jvmTest`,
      `:funktor-demo:server:test`, plus whole-project `assemble` (all main sources incl. all JS apps)

## Review record (filled by /feature-review 2026-07-25)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | PASS after fixes | 2 HIGH, 4 MEDIUM, 5 LOW. No regex over-reach, no misplaced/duplicate import, no stale KDoc, and zero surviving untyped `UserId`-vs-`String` kotest assertions in the scripted diff. |
| 2. Domain expert | PASS | Identity model correct; placement in `ultra:security` verified as FORCED (funktor:saas has no path to funktor:auth); wire+storage byte-identical on BOTH serializers and both backends; index semantics untouched; Monko raw-driver audit complete (one `Filters.eq`, correctly `.value`). |
| 3. Security | PASS — no auth bypass | Ownership check is real full-string equality no bare key/foreign-realm id/sentinel can satisfy; CSRF hash byte-identical AND now stronger; session revocation fail-closed; last-owner invariant unaffected by `Set<UserId>` keying. |

### Fixes applied

1. **`funktor/auth/build.gradle.kts` → `api(project(":ultra:security"))`** (HIGH, reviewers 1+2).
   `UserId` is in this module's PUBLISHED API (`AuthSetPasswordRequest.userId`, `tokenUserId`,
   `AuthRecord.ownerId`, …); `implementation` would leave an external consumer unable to resolve it.
   Only compiled in-repo by luck via `funktor:core`'s `api`.
2. **Request-body deserialization now returns 400, not 500** (MEDIUM, all three reviewers).
   `AuthSetPasswordRequest.userId` was the first value class on an inbound BODY; a violated `init`
   arrived as `InvocationTargetException`, which is neither `AwakerException` nor
   `CouldNotConvertException` → 500 + internal-error log per request (+ stack trace outside prod).
   Fixed at the single choke point `SlumberRestCodec.deserialize` (`awakeBody`), deliberately narrow:
   only a CONSTRUCTOR-thrown `IllegalArgumentException`/`IllegalStateException` is translated, so a
   genuine server bug still surfaces as a 500. Mirrors `IncomingValueClassConverter` on the URI path.
   **This is the boundary that Steps 3 (`orgId`) and 4 (`Email`, much stricter `init`) also need.**
   Pinned by `funktor/rest/src/jvmTest/kotlin/codec/SlumberRestCodecSpec.kt`.
3. **The degradation is now TOTAL** (MEDIUM, reviewers 1+3). `JwtGenerator.extractUser` returns a real
   `UserRecord.Anonymous` + `UserPermissions.anonymous` when the id degrades — previously it produced a
   `LoggedIn` record that reported `isAnonymous() == true` while still carrying the token's full
   permissions (identity and permissions are read from INDEPENDENT claim sets). Pre-existing for the
   permission half; the inconsistent record was new. Now fail-closed on both halves.
4. **`Payload.extractUser` falls back to `sub` when the id claim is present-but-invalid** — was only
   falling back when the claim was absent, losing the original chain's intent.
5. **`ExtractUserSpec` added** (HIGH, reviewer 1 — the headline security change shipped untested).
   Asserts `isAnonymous()` directly, since `AuthRule.isAuthenticated` derives from it.
6. **`UserIdSpec` strengthened to a real slumber round-trip** (MEDIUM, reviewer 1) — the descriptor
   assertion proved neither the encoded form nor, critically, that `init` RUNS ON DECODE. Slumber is on
   the test classpath and is the codec that actually carries `UserId` to the wire and both DBs.
7. **`AuthUserAdapter.loadById(id: UserId)`** (MEDIUM, all three) — the last "thread it everywhere" gap,
   and the exact spot where the coll/key semantics matter most. KDoc now documents the Monko
   `ensureKey` hazard and points at the round-trip guard. (Surfaced 3 more untyped `shouldBe` assertions.)
8. `UserId.parseOrNull` added (LOW) — de-duplicates the `runCatching` at the two token-parsing
   boundaries and narrows the catch from `Throwable` to `IllegalArgumentException`.
9. Control-char ban widened to C0 + DEL + C1 + U+2028/U+2029 to match its KDoc (LOW).
10. 4 redundant same-package imports removed; `ChangePasswordWidget`'s self-shadowing local renamed (LOW).

### Deferred (recorded, not fixed here)

- **Monko `findById` silently coerces a foreign-collection `_id` to a bare key; Karango rejects it.**
  Pre-existing backend asymmetry, NOT exploitable today (three independent guards hold). Framework-wide
  behaviour change → own gated feature: `.claude/future-plans/20260725-monko-findbyid-collection-prefix.md`
- **Dart codegen has no value-class case** → a generated client would type `UserId` as `dynamic`.
  Dormant (no app calls `dartProject(...)`). Recorded in the same future-plan doc's neighbourhood; revisit
  if/when Dart codegen is used.
- **`ownerIdsOf` (`ultra/security/.../user/OrgRole.kt`) is DEAD SURFACE** — only its own spec references it;
  both real callers compute the owner set inline from shapes that don't even match its signature. Per the
  standing "flag dead surface before porting" rule this is raised for the user to decide (delete vs keep),
  NOT removed unilaterally. `wouldRemoveLastOwner` alongside it IS used by both callers — keep.
- Reuse question (reviewer 1): keep the ~38 explicit `UserId(x._id)` conversions rather than a generic
  `Stored<*>.asUserId()` helper, which would type-check for `Organisation`/`OrgMember` too and silently
  mint a bogus identity. A narrow `val Stored<out AuthUser>.userId: UserId` is the sound version if the
  repetition ever grates — optional follow-up, not a blocker.

**Red-team follow-up**: `.claude/tasks/20260725-redteam-value-class-userid.md` (7 scenarios, COLLECTED
not executed).
