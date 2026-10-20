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
    /** Lifetime of email-verification tokens issued at sign-up; never beyond [pendingAccountLifetime]. */
    val emailVerificationTokenLifetime: Duration = 24.hours,
    /**
     * How long a never-activated account lives. After it the account is retired — when sign-up, SSO, activation
     * or reset next touch it (resend refuses it; a password sign-in still answers `ActivationRequired`), or by
     * `funktor/all`'s `PendingAccountsExpiryWorker` — and its address is free for a new sign-up; see
     * `AuthRealm.retirePendingAccount`. Also bounds how much mail resend can cause.
     */
    val pendingAccountLifetime: Duration = 7.days,
    /**
     * The growing wait before each activation resend: the first after the sign-up mail waits the first
     * entry, the next the second, and the last entry repeats. Must not be empty.
     *
     * Per pending account, stored on its marker. It bounds the mail an account's own password holder can
     * cause — including an attacker who pre-registered someone else's address.
     */
    val activationResendDelays: List<Duration> = listOf(
        5.minutes, 15.minutes, 1.hours, 4.hours, 12.hours, 24.hours,
    ),
    /**
     * Lifetime of the single-use token handed out when a sign-in is refused for a pending account.
     *
     * Short on purpose: it exists only to carry the user from the login page to the activation page.
     */
    val activationResendTokenLifetime: Duration = 15.minutes,
    /** Lifetime of password-recovery tokens. */
    val passwordRecoveryTokenLifetime: Duration = 1.hours,
    /** Lifetime of the short-lived org-selection token issued between credential-check and org pick. */
    val orgSelectionTokenLifetime: Duration = 5.minutes,
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
) {
    init {
        require(activationResendDelays.isNotEmpty()) { "activationResendDelays must not be empty" }
    }

    /** The wait before the resend that follows [resends] earlier ones. */
    fun activationResendDelay(resends: Int): Duration =
        activationResendDelays[resends.coerceIn(0, activationResendDelays.lastIndex)]
}
