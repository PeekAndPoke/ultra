package io.peekandpoke.funktor.auth

/**
 * Configuration for automatic session lifecycle management in [AuthState].
 *
 * When enabled, [AuthState] will:
 * - Periodically check if the JWT token is about to expire
 * - Automatically refresh the token before it expires
 * - Check token validity when the browser window regains focus
 * - Handle session expiry gracefully (logout + redirect to login)
 */
data class AuthSessionConfig(
    /** Whether session lifecycle management is enabled. */
    val enabled: Boolean = true,
    /** Interval in milliseconds between token expiry checks. Default: 60_000 (1 minute). */
    val checkIntervalMs: Int = 60_000,
    /** Refresh the token when it expires within this many milliseconds. Default: 120_000 (2 minutes). */
    val refreshBeforeExpiryMs: Long = 120_000L,
    /** Check token validity when the browser window regains focus. Default: true. */
    val checkOnWindowFocus: Boolean = true,
    /** Custom handler when session expires. Null = default behavior (logout + redirect to login). */
    val onSessionExpired: (() -> Unit)? = null,
    /** Called after the token is successfully refreshed. Use for refreshing ApiAcl etc. */
    val onTokenRefreshed: (() -> Unit)? = null,
) {
    companion object {
        /** A config that disables all session lifecycle management. */
        val disabled = AuthSessionConfig(enabled = false)
    }
}
