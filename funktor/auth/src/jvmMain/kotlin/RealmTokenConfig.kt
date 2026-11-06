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
     * or reset next touch it, or by `funktor/all`'s `PendingAccountsExpiryWorker` — and its address is free for a
     * new sign-up; see `AuthRealm.retirePendingAccount`. Also bounds how much mail signing up again can cause.
     */
    val pendingAccountLifetime: Duration = 7.days,
    /**
     * The growing wait before each re-sent activation mail — signing up again with a pending address re-sends
     * it: the first after the sign-up mail waits the first entry, the next the second, and the last entry
     * repeats. Must not be empty. Inside the wait a sign-up again sends nothing, and answers as always.
     *
     * Per pending account, stored on its marker. Anyone who knows the address can sign it up again, so this
     * is what bounds the mail they can cause to it.
     */
    val activationResendDelays: List<Duration> = listOf(
        5.minutes, 15.minutes, 1.hours, 4.hours, 12.hours, 24.hours,
    ),
    /**
     * How long the OLDER activation links stay usable after a re-send: each gets the earlier of its own expiry and
     * now plus this; none is ever extended. Someone signing the address up again then cannot kill the link a user is
     * typing a password into. 0: only the newest link lives.
     */
    val activationLinkOverlap: Duration = 30.minutes,
    /**
     * The most cancel links ("Didn't sign up? Cancel") one pending account keeps. Each activation mail carries its
     * own, which lives as long as the account ([pendingAccountLifetime]); past this many the OLDEST goes, so the
     * newest mails' links always work. With the default delays, 7 days hold the sign-up mail and 11 re-sends (the
     * last after about 6.7 days), so the default 12 keeps every mailed link. One delay slot sends one mail, however
     * many sign-ups race for it (ULTRA-00130). At least 2.
     */
    val maxSignUpCancelTokens: Int = 12,
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
    /** Length in bytes of randomly generated tokens (verification, recovery, email-change); 32 at least. */
    val randomTokenByteLength: Int = 256,
) {
    init {
        require(activationResendDelays.isNotEmpty()) { "activationResendDelays must not be empty" }
        require(!activationLinkOverlap.isNegative()) { "activationLinkOverlap must not be negative" }
        // 2 at least: a re-send keeps the token it just minted and the previous mail's, so the link a user may be
        // reading when a new mail goes out keeps working.
        require(maxSignUpCancelTokens >= 2) { "maxSignUpCancelTokens must be at least 2" }
        // The stored hash is unsalted and fast (`TokenHash`): only a long random token makes it unguessable.
        require(randomTokenByteLength >= 32) { "randomTokenByteLength must be at least 32" }
    }

    /** The wait before the re-sent mail that follows [resends] earlier ones. */
    fun activationResendDelay(resends: Int): Duration =
        activationResendDelays[resends.coerceIn(0, activationResendDelays.lastIndex)]
}
