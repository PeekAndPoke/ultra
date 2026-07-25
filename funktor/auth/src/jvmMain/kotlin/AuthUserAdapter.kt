package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.JsonObject

/**
 * OPERATIONS on a realm's users — loading, creating, wire-serialization.
 *
 * This is the operations seam of the auth system, provided once per realm ([AuthRealm.users]).
 * New user operations (update, GDPR erasure, admin search, ...) are added HERE, so the realm
 * interface stops growing. Intrinsic user DATA is read via the [AuthUser] bound instead.
 */
interface AuthUserAdapter<USER : AuthUser> {

    /**
     * Parameters for creating a new user during sign-up, see [createForSignup].
     */
    @ConsistentCopyVisibility
    data class CreateUserForSignupParams private constructor(
        val email: String,
        val displayName: String,
    ) {
        companion object {
            fun of(email: String, displayName: String? = null) = CreateUserForSignupParams(
                email = email.trim().lowercase(),
                displayName = displayName?.trim() ?: email.substringBefore("@").trim(),
            )
        }
    }

    /**
     * Loads a user by its id.
     *
     * [id] is the realm-qualified Vault `_id` (`<user collection>/<key>`). Implementations should
     * pass `id.value` straight to their repository. NOTE that `Repository.findById` reduces an id to
     * its bare `_key` on the Monko backend, so an `_id` from ANOTHER realm's collection can resolve
     * a same-key document here — see `.claude/future-plans/20260725-monko-findbyid-collection-prefix.md`.
     * Callers that use the result as an authorization subject must round-trip it
     * (`?.takeIf { it._id == id.value }`), as `B2bMembersApi.b2bUserOf` does.
     */
    suspend fun loadById(id: UserId): Stored<USER>?

    /** Loads a user by its email. */
    suspend fun loadByEmail(email: String): Stored<USER>?

    /** Creates a new user during sign-up. Providers call this for the given email/displayName. */
    suspend fun createForSignup(params: CreateUserForSignupParams): Stored<USER>

    /** Serializes the given user for the wire (e.g. the sign-in response payload). */
    suspend fun serialize(user: Stored<USER>): JsonObject
}
