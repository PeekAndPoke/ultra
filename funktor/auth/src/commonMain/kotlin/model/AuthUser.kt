package io.peekandpoke.funktor.auth.model

/**
 * Contract every auth-realm USER type must fulfil: `AuthRealm<USER : AuthUser>`.
 *
 * This is the DATA seam of the auth system — intrinsic facts about a user that auth flows read
 * directly (`user.value().email`) instead of going through realm accessor methods or casts.
 *
 * Keep this interface TINY (data-only, universal-only). The bloat firewalls:
 * - default getters make additions non-breaking for existing user classes,
 * - new configuration values extend value objects like [LanguageSettings], never this interface.
 *
 * Optional capabilities (e.g. org memberships) stay separate opt-in `Has*` interfaces. OPERATIONS
 * on users (loading, creating, serializing) live on `AuthUserAdapter`, not here.
 */
interface AuthUser {
    /** The user's email address. */
    val email: String

    /** Display name for messages and UIs; `null` means "none set" — fall back to [email]. */
    val displayName: String? get() = null

    /** The user's language settings, see [LanguageSettings]. */
    val language: LanguageSettings get() = LanguageSettings.default
}
