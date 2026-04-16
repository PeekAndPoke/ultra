# V1 Email Auth Flows + Session Records

**Status:** PLANNED 2026-04-16
**Hard goal for v1.0.0** — full email auth flow with session records.

## Context

Funktor already ships:

- `AuthRecord.Password` and `AuthRecord.PasswordRecoveryToken` records
- `signUp`, `signIn`, `setPassword`, `recoverAccountInitPasswordReset`,
  `recoverAccountValidatePasswordResetToken`, `recoverAccountSetPasswordWithToken`
- `JwtGenerator` (HMAC512 signed JWTs with user + permissions claims)
- `messaging.sendPasswordChangedEmail`, `sendPasswordRecoveryEmil` (typo: "Emil")
- Realm-level `signUp` notes `requiresActivation = true` but `AuthSystem.activate()` is unimplemented

What's MISSING for v1:

1. Email verification on sign-up (token issuance + activate impl + email)
2. Email change flow (request + confirm)
3. Session records as the JWT-backed revocation mechanism
4. New-device / new-IP login notifications
5. Demo pages exercising every flow end-to-end

## Architecture decisions (locked-in)

- **Transport:** JWT in `Authorization: Bearer …` header. No cookies.
- **Revocation:** Session record per active login in DB. JWT carries the `sessionId` claim. Each request validates the
  session exists in DB.
- **Hot-path cache:** `sessionId → user` cached in memory for 30s per JVM. ~2 DB hits/min/session/JVM. Use
  `ultra/cache`.
- **Cross-JVM cache invalidation:** none — accept the 30s TTL window.
- **Device tracking:** hash of `userAgent + IP` stored on the session row. New fingerprint for a user → "new device"
  email.
- **Token / session lifetime:** configurable **per realm**. JWT TTL = session TTL by default. Different realms (e.g.
  internal admin vs. public user) can have different policies. See `RealmTokenConfig` below.

## Backend work

### 1. New domain records (funktor/auth/.../domain/AuthRecord.kt)

```kotlin
data class EmailVerificationToken(
    override val realm: String,
    override val ownerId: String,
    override val expiresAt: Long,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
    override val token: String,
) : AuthRecord { /* Polymorphic.TypedChild "email-verification-token" */ }

data class EmailChangeToken(
    override val realm: String,
    override val ownerId: String,
    override val expiresAt: Long,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
    override val token: String,
    /** The new email the user wants to switch to */
    val pendingEmail: String,
) : AuthRecord { /* Polymorphic.TypedChild "email-change-token" */ }

data class Session(
    override val realm: String,
    override val ownerId: String,
    override val expiresAt: Long,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
    /** The session id — embedded as a JWT claim. */
    override val token: String,
    /** Hash of (userAgent + ipAddress). Used to detect new-device logins. */
    val deviceFingerprint: String,
    val userAgent: String?,
    val ipAddress: String?,
    val lastSeenAt: MpInstant,
) : AuthRecord { /* Polymorphic.TypedChild "session" */ }
```

All three types fit the existing `AuthRecord` sealed interface — no schema changes to the auth records repo, just new
polymorphic children.

### 2. SessionStore (new file: funktor/auth/.../SessionStore.kt)

```kotlin
interface SessionStore {
    suspend fun create(
        realm: String, ownerId: String, deviceFingerprint: String,
        userAgent: String?, ipAddress: String?, ttl: Duration
    ): Stored<Session>
    suspend fun getById(sessionId: String): Stored<Session>?  // cached
    suspend fun touch(sessionId: String)                       // updates lastSeenAt
    suspend fun listForUser(ownerId: String): List<Stored<Session>>
    suspend fun revoke(sessionId: String)
    suspend fun revokeAllForUser(ownerId: String, except: String? = null)

    class Cached(
        private val inner: SessionStore,
        private val cache: Cache<String, Stored<Session>>,  // 30s TTL
    ) : SessionStore { /* delegate + invalidate on revoke/touch */ }

    class Vault(private val authRecords: AuthRecordStorage) : SessionStore { /* impl */ }
}
```

### 3. Per-realm token configuration

Add a `RealmTokenConfig` with sensible defaults, exposed on `AuthRealm`:

```kotlin
data class RealmTokenConfig(
    /** Lifetime of an active session row (and the JWT it backs). */
    val sessionLifetime: Duration = 30.days,
    /** Lifetime of email-verification tokens. */
    val emailVerificationTokenLifetime: Duration = 24.hours,
    /** Lifetime of password-recovery tokens (currently hardcoded to 1h in EmailAndPasswordAuth). */
    val passwordRecoveryTokenLifetime: Duration = 1.hours,
    /** Lifetime of email-change confirmation tokens. */
    val emailChangeTokenLifetime: Duration = 24.hours,
    /** How often to debounce session.lastSeenAt writes — avoids per-request DB write. */
    val sessionTouchInterval: Duration = 1.minutes,
    /** Cache TTL for SessionStore.Cached. */
    val sessionCacheTtl: Duration = 30.seconds,
)

interface AuthRealm<USER> {
    // ...
    val tokenConfig: RealmTokenConfig get() = RealmTokenConfig()
}
```

Each provider/flow reads these from `realm.tokenConfig` rather than hardcoding (replaces the existing
`// TODO: make ... configurable` comments in `EmailAndPasswordAuth`).

### 4. JWT changes (funktor/auth/.../AuthRealm.kt — `generateJwt`)

Add a `sessionId` claim (custom claim namespace `funktor:sid`). The `extractSessionId(payload)` helper goes on
`JwtGenerator` (small extension in `ultra/security`). JWT `expiresInMinutes(...)` driven by
`realm.tokenConfig.sessionLifetime`.

### 5. Auth middleware (funktor/auth/.../middleware/SessionAuth.kt — new)

Ktor plugin/feature:

- Read `Authorization: Bearer <jwt>`
- Verify signature via `JwtGenerator`
- Extract `sessionId` claim
- Look up via `SessionStore.getById` (cached)
- If null → 401
- Else → attach `User` to call attributes (existing pattern), bump `lastSeenAt` async (debounced — don't write on every
  request; touch every N seconds)

Reject conditions: missing header, bad signature, expired JWT, missing/revoked session.

### 6. AuthSystem extensions

```kotlin
// In AuthSystem
suspend fun verifyEmail(realm: String, token: String): VerifyEmailResponse
suspend fun requestEmailChange(realm: String, userId: String, newEmail: String): RequestEmailChangeResponse
suspend fun confirmEmailChange(realm: String, token: String): ConfirmEmailChangeResponse
suspend fun listSessions(realm: String, userId: String): List<SessionModel>
suspend fun revokeSession(realm: String, sessionId: String): Unit
suspend fun revokeAllSessions(realm: String, userId: String, except: String? = null): Unit
```

### 7. Wiring into existing flows

- **`signUp` (realm)** → if `requiresActivation`, create `EmailVerificationToken`, send verification email. Don't
  auto-issue session/JWT yet — user must verify first.
- **`signIn` (provider/realm)** → after credential check: create `Session` row, fingerprint device, check if new
  device → fire `sendNewDeviceLoginEmail`, generate JWT carrying `sessionId`. On unverified user, return
  `requiresActivation` instead.
- **`setPassword`** → after success, `revokeAllSessions(except = currentSessionId)` — keep the current session, kill
  others. Send "password changed" email (already exists).
- **`recoverAccountSetPasswordWithToken`** → revoke ALL sessions (no current).

### 8. Email templates (funktor/auth/.../AuthRealm.kt — extend `Messaging<USER>`)

```kotlin
suspend fun sendEmailVerificationEmail(user: Stored<USER>, verifyUrl: String): EmailResult
suspend fun sendEmailChangeConfirmationEmail(user: Stored<USER>, newEmail: String, confirmUrl: String): EmailResult
suspend fun sendNewDeviceLoginEmail(user: Stored<USER>, device: SessionModel): EmailResult
```

Default impls in `DefaultMessaging` mirror the existing simple HTML templates. Also fix the "Emil" typo (rename to
`sendPasswordRecoveryEmail`, keep old name as `@Deprecated` for one release).

### 9. New API endpoints (funktor/auth/.../api/AuthApi.kt)

```
POST /api/auth/verify-email           — body: { token }
POST /api/auth/email-change/request   — authed, body: { newEmail }
POST /api/auth/email-change/confirm   — body: { token }
GET  /api/auth/sessions               — authed, returns own sessions
DELETE /api/auth/sessions/:sessionId  — authed (own session or super)
DELETE /api/auth/sessions             — authed, revoke all (except current)
POST /api/auth/logout                 — authed, revoke current session
```

### 10. Tests (both DB backends, via existing `AuthRecordStorageBaseSpec` pattern)

- Sign-up creates an `EmailVerificationToken`; verify consumes it; double-verify fails.
- Email change: request creates a token + sends to new address; confirm consumes + updates; old token can't be reused.
- Session lifecycle: signIn creates session; revoke removes it; subsequent JWT use fails.
- Cache: hot path doesn't hit DB; revoke invalidates within TTL.
- Password change revokes other sessions, keeps current.
- New device hook fires on first-seen fingerprint, doesn't fire on re-seen.
- Auth middleware: no header → 401; bad sig → 401; expired JWT → 401; revoked session → 401.

## Demo work (funktor-demo)

Update `funktor-demo/adminapp` (or add a `userapp` if cleaner) with full Kraft-based pages:

| Route                                   | Auth   | Behavior                                                                      |
|-----------------------------------------|--------|-------------------------------------------------------------------------------|
| `/signup`                               | Public | Email + password form; on success → "Check your inbox"                        |
| `/verify-email?token=…`                 | Public | Calls `verify-email` endpoint; success → redirect to login                    |
| `/login`                                | Public | Email + password; success → store JWT in localStorage; redirect to `/profile` |
| `/forgot-password`                      | Public | Email entry → "If an account exists, we sent a link"                          |
| `/reset-password?token=…`               | Public | New password form; success → "Sign in again"                                  |
| `/profile`                              | Authed | Show user info; links to email change, password change, sessions              |
| `/profile/email-change`                 | Authed | New email entry → "Check your inbox"                                          |
| `/profile/email-change/confirm?token=…` | Authed | Calls confirm endpoint; success → "Email updated"                             |
| `/profile/password-change`              | Authed | Old + new password fields                                                     |
| `/profile/sessions`                     | Authed | List with device + last-seen + revoke buttons                                 |
| `/logout`                               | Authed | Revoke current → clear JWT → redirect to login                                |

All pages live under `funktor-demo/userapp/src/jsMain/kotlin/pages/auth/` (new module if needed). Server-side routes
wire to the existing `AuthApi`.

## Phased execution

To make this tractable, ship in phases, each green and merged before the next:

**Phase 1 (foundation):** Domain records + `SessionStore` + cache + JWT `sessionId` claim. Tests for SessionStore CRUD
and cache. (~½ day)

**Phase 2 (middleware):** Auth middleware that reads JWT → resolves session → attaches user. Wire into demo backend.
Tests for middleware happy + reject paths. (~½ day)

**Phase 3 (signIn/signUp wiring):** signIn creates session; signUp issues verification token. New device detection +
email. Tests. (~1 day)

**Phase 4 (email verification + email change):** Backend + endpoints + emails. Tests. (~1 day)

**Phase 5 (session management API):** list/revoke endpoints + password-change-revokes-others wiring. Tests. (~½ day)

**Phase 6 (demo pages):** All pages above, working end-to-end against the local funktor-demo. Manual verification. (~2
days)

**Total:** ~5–6 working days.

## Out of scope for v1 (post-v1 backlog)

- MFA / TOTP
- Account deletion with grace period
- Recovery codes
- Device trust list (mark-as-trusted)
- Suspicious-IP magic re-auth
- Cross-JVM cache invalidation (e.g. via Funktor pub/sub or Redis)
- Sliding session windows (auto-extend on activity)
- Migration to short-lived JWT + refresh-token rotation (the session+cache approach can be extended later if needed)

## Files to add / modify

**New:**

- `funktor/auth/src/jvmMain/kotlin/SessionStore.kt`
- `funktor/auth/src/jvmMain/kotlin/middleware/SessionAuth.kt`
- `funktor/auth/src/jvmMain/kotlin/api/SessionsApi.kt`
- `funktor/auth/src/jvmTest/kotlin/SessionStoreSpec.kt`
- `funktor/auth/src/jvmTest/kotlin/SessionAuthMiddlewareSpec.kt`
- `funktor/auth/src/jvmTest/kotlin/EmailVerificationFlowSpec.kt`
- `funktor/auth/src/jvmTest/kotlin/EmailChangeFlowSpec.kt`
- `funktor-demo/userapp/...` (or extend adminapp) — pages above

**Modify:**

- `funktor/auth/src/jvmMain/kotlin/domain/AuthRecord.kt` — add `EmailVerificationToken`, `EmailChangeToken`, `Session`
- `funktor/auth/src/jvmMain/kotlin/AuthSystem.kt` — add the 6 new methods
- `funktor/auth/src/jvmMain/kotlin/AuthRealm.kt` — extend `Messaging<USER>` interface, fix "Emil" typo
- `funktor/auth/src/jvmMain/kotlin/provider/EmailAndPasswordAuth.kt` — wire signIn/signUp into sessions + verification
- `funktor/auth/src/jvmMain/kotlin/api/AuthApi.kt` — add new endpoints
- `funktor/auth/src/jvmMain/kotlin/index_jvm.kt` — kontainer registration for SessionStore + middleware
- `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt` — `withSessionId` / `extractSessionId` helpers
- `funktor-demo/server/src/main/kotlin/server.kt` — install middleware
- `funktor-demo/server/src/main/kotlin/api/...` — wire AuthApi if not already

## Verification

Each phase: `./gradlew :funktor:auth:jvmTest` + integration via funktor-demo with both DB backends.

Final: full manual click-through of all demo pages on a fresh database for each of: sign-up → verify → login → change
password → email change → revoke session → logout.
