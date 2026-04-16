package io.peekandpoke.funktor.auth

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Per-realm configuration for token and session lifetimes.
 *
 * Each [AuthRealm] exposes one of these via `tokenConfig` so different realms (e.g. an internal
 * admin realm vs. a public-user realm) can have different security postures without forcing a
 * global setting.
 */
data class RealmTokenConfig(
    /**
     * Lifetime of an active session row and the JWT it backs. JWTs are renewed via
     * `refreshToken` before they expire; revoking the session row logs the user out within
     * one [sessionCacheTtl] window.
     */
    val sessionLifetime: Duration = 30.days,
    /** Lifetime of email-verification tokens issued at sign-up. */
    val emailVerificationTokenLifetime: Duration = 24.hours,
    /** Lifetime of password-recovery tokens. */
    val passwordRecoveryTokenLifetime: Duration = 1.hours,
    /** Lifetime of email-change confirmation tokens sent to the *new* address. */
    val emailChangeTokenLifetime: Duration = 24.hours,
    /**
     * Debounce window for `Session.lastSeenAt` writes. Inside this window only the in-memory
     * cache value is updated; persistence is deferred. Avoids a per-request DB write.
     */
    val sessionTouchInterval: Duration = 1.minutes,
    /**
     * In-memory TTL for session lookups in [SessionStore.Cached]. Determines the worst-case
     * window between `revoke()` and the user actually being logged out on a given JVM.
     */
    val sessionCacheTtl: Duration = 30.seconds,
    /** Length in bytes of randomly generated tokens (verification, recovery, email-change). */
    val randomTokenByteLength: Int = 256,
)
