# Auth USER seams refactor — AuthUser bound + AuthUserAdapter

**Status:** TODO
**Plan:** design agreed in-session 2026-07-22; prerequisite for `20260722-i18n-s7-emails.md` (S7)
**Security-critical:** no (behavior-preserving refactor of auth code — full 3-agent gate anyway)

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
    val serializer: KSerializer<USER>          // replaces serializeUser — a value, not a method
    suspend fun loadById(id: String): Stored<USER>?
    suspend fun loadByEmail(email: String): Stored<USER>?
    suspend fun createForSignup(params: CreateUserForSignupParams): Stored<USER>
}
```

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
- [ ] `AuthUserAdapter<USER>` with `serializer`, `loadById`, `loadByEmail`, `createForSignup`
- [ ] `AuthRealm<USER : AuthUser>` rebound; realm gets `users: AuthUserAdapter<USER>`
- [ ] Removed from the realm interface: `getUserEmail`, `serializeUser`, `loadUserById`,
      `loadUserByEmail`, `createUserForSignup` (call sites → bound reads / adapter)
- [ ] `DefaultMessaging` reads `user.value.email` directly
- [ ] Migrated: `AdminUserRealm`, `B2bRealm`, `OperatorRealm` (funktor-demo/server) + auth test
      fixture (`funktor/auth/src/jvmTest/kotlin/index_jvmTest.kt`); user models implement `AuthUser`
- [ ] Behavior-preserving: NO functional change; existing auth e2e suite green unchanged

## Test evidence

- [ ] Existing auth/backend e2e suites pass unchanged (the point of the refactor)
- [ ] Compile across jvm+js (user models are commonMain)

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |
