# Auth USER seams refactor — AuthUser bound + AuthUserAdapter

**Status:** DONE (2026-07-22) — review loop closed in round 1 with zero confirmed findings across all
three reviewers; 1,028 backend tests green
**Plan:** design agreed in-session 2026-07-22; prerequisite for `20260722-i18n-s7-emails.md` (S7)
**Security-critical:** YES (user, 2026-07-22: "this is our security infra and we need this absolutely
bullet-proof") — even though behavior-preserving, this touches the auth core.

## Review protocol (user directive, 2026-07-22)

Deep review LOOP, not a single pass — cost is explicitly not a constraint:

1. Full 3-agent gate (impl+style, domain, security) on the diff — all `opus`, security reviewer at
   `xhigh` effort; every finding adversarially verified by the coordinator before acceptance.
2. Implement ALL confirmed fixes (no severity threshold — confirmed MEDIUM/LOW get fixed too, not
   deferred).
3. Re-run the FULL gate on the updated diff (fresh reviewers, whole diff — not just the fix delta).
4. Repeat 2–3 until a round produces ZERO confirmed findings. Only then: tests green → DONE.

## Problem

`AuthRealm<USER>` accumulates one method per USER concern — `getUserEmail`, `serializeUser`,
`loadUserById`, `loadUserByEmail`, `createUserForSignup`, `getMemberships` (via
`user.value() as? HasOrgMemberships` cast) — an ever-growing interface with unsafe casts and
ceremony around plain field reads (`AuthRealm.kt:200-215`). Each new user-related feature (next:
S7's messaging language) would add yet another accessor.

## Design (agreed 2026-07-22)

Split the seams by their nature — three buckets, each with a home:

### 1. Intrinsic user DATA → tiny bound interface `AuthUser`

```kotlin
// commonMain (user models are shared with the frontend; host near HasOrgMemberships)
interface AuthUser {
    val email: String
    val displayName: String? get() = null
    val language: LanguageSettings get() = LanguageSettings.default
}

interface AuthRealm<USER : AuthUser> { ... }   // user.value.email — no cast, no suspend, no accessor
```

Bloat firewalls (the discipline that keeps this ~3 members forever):
- **Default getters** — additions are non-breaking for existing user classes.
- **Extensible value objects** — new config values extend `LanguageSettings`-style objects, never the
  interface. `LanguageSettings` v1: `messaging: String?` (email/notification language). Likely future
  members: timezone (S7 date formatting wants recipient tz — consider naming it `LocaleSettings`),
  display language, notification prefs. Serializable, commonMain.
- **Data-only, universal-only.** Anything not needed by every auth flow stays out.

### 2. Optional capabilities → keep small `Has*` interfaces

`HasOrgMemberships` (ultra:security) stays as-is — org membership is a SaaS-layer concern, not
universal auth; opt-in via cast is the right semantics there. Future: permissions/roles.

### 3. OPERATIONS on users → `AuthUserAdapter<USER>` (provided once at realm construction)

```kotlin
interface AuthUserAdapter<USER : AuthUser> {
    suspend fun loadById(id: String): Stored<USER>?
    suspend fun loadByEmail(email: String): Stored<USER>?
    suspend fun createForSignup(params: CreateUserForSignupParams): Stored<USER>   // params move here from AuthRealm
    suspend fun serialize(user: Stored<USER>): JsonObject
}
```

**Correction (2026-07-22, after call-site inventory):** `serializeUser` stays a METHOD (`serialize`)
on the adapter, not a `KSerializer<USER>` value — every existing realm serializes a DTO
(`user.asApiModel()` → `AdminUserModel.serializer()` etc.), not the entity, so a bare entity
serializer cannot reproduce the wire format behavior-preservingly.

The realm delegates; its own interface STOPS growing. `Messaging<USER>` already follows this
extraction pattern — end state: realm = config + `users: AuthUserAdapter` + `messaging: Messaging` +
the actual auth flows. Explicitly NOT on the adapter: data reads (`emailOf(user)`) — that would just
relocate accessor-per-need and lose the compile-time guarantee.

### Future seams — validation that the buckets hold

| Coming seam | Bucket |
|---|---|
| Timezone (S7 email date formatting) | `LanguageSettings`/`LocaleSettings` member |
| Account status (locked/disabled/verified) | future `status` value object on `AuthUser` |
| Notification preferences | settings value object |
| Consent/legal (ToS version, GDPR) | value object |
| Save/update user (needed by `20260720-password-rehash-on-login.md`, profile UI) | adapter method |
| GDPR delete/anonymize | adapter method |
| Admin listing/search | adapter method |
| Permissions/roles | `Has*` capability |

None of these touch `AuthRealm` again.

## Spec / acceptance

- [ ] `LanguageSettings` (commonMain, serializable, `default`; final name decided at impl —
      `LocaleSettings` if timezone is expected to join)
- [ ] `AuthUser` (commonMain): `email`, `displayName` (default null), `language` (default)
- [ ] `AuthUserAdapter<USER>` with `loadById`, `loadByEmail`, `createForSignup`, `serialize`
      (+ `CreateUserForSignupParams` moves in from `AuthRealm`)
- [ ] `AuthRealm<USER : AuthUser>` rebound; realm gets `users: AuthUserAdapter<USER>`
- [ ] Bound ripple: `AuthProvider`'s unbounded `<USER>` methods (+ overrides in
      `EmailAndPasswordAuth`, `GoogleSsoAuth`, `GithubSsoAuth`) → `<USER : AuthUser>`;
      `AuthSystem.realms: List<AuthRealm<Any>>` → `List<AuthRealm<*>>`
- [ ] Removed from the realm interface: `getUserEmail`, `serializeUser`, `loadUserById`,
      `loadUserByEmail`, `createUserForSignup` (call sites → bound reads / adapter)
- [ ] `DefaultMessaging` reads `user.value().email` directly
- [ ] Migrated (ALL SIX impls, per inventory): `AdminUserRealm`, `B2bRealm`, `OperatorRealm`
      (funktor-demo/server), `TestUserRealm` (funktor/all jvmTest), `TestAppUserRealm` +
      `MinimalTestRealm` (funktor/auth jvmTest — `MinimalTestRealm : AuthRealm<Any>` needs a stub
      `AuthUser` type since `Any` cannot satisfy the bound); user models implement `AuthUser`; the
      3 demo user models gain a stored `language: LanguageSettings = LanguageSettings.default`
- [ ] Behavior-preserving: NO functional change; existing auth e2e suite green unchanged

## Test evidence

- [ ] Existing auth/backend e2e suites pass unchanged (the point of the refactor)
- [ ] Compile across jvm+js (user models are commonMain)

## Review record (review loop, round 1 — 2026-07-22)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | PASS | 0 — verified value()/resolve()/.value equivalence, byte-identical adapter bodies, vault codec defaults for the new stored `language` field (old docs → `LanguageSettings.default`, no migration), TestUser wire format unchanged (interface-default getters not serialized), complete bound propagation, no positional-caller hazard from mid-constructor `language` insertion, clean imports |
| 2. Domain expert | PASS | 0 — bucket split holds vs S7/rehash/GDPR; placement correct; frontend `AuthState<USER>` correctly unbounded (wire-model USER); inline adapter matches `DefaultMessaging` convention |
| 3. Security | PASS | 0 — flow-by-flow equivalence (signIn normalization, signUp TOCTOU guard, recovery consume-before-write, refresh cross-realm check, selectOrg single-use token); `language` reaches no wire payload/JWT; star projection safer than old `List<AuthRealm<Any>>` |

Loop terminated after round 1 (zero-findings round). Tests: 1,028 backend tests green
(funktor:auth 86 incl. DB-backed storage specs, funktor:all e2e 105, saas 20, rest 52, core 765);
all modules compile jvm+js.

**Forward notes from reviewers (carried into follow-on tasks, not this diff):**
- Future `AuthUserAdapter` ops (update/GDPR-erase/search) should ship with `error("not supported")`
  default implementations so additions stay non-breaking across the 6+ impls.
- Pre-existing (NOT introduced here), recorded for the security backlog: recovery/SSO email lookups
  are un-normalized (fails closed); recovery-init has a timing side-channel (email send only when
  user exists).
