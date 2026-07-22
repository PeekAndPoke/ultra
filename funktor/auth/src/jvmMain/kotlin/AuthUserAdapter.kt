package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.model.AuthUser
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

    /** Loads a user by its id. */
    suspend fun loadById(id: String): Stored<USER>?

    /** Loads a user by its email. */
    suspend fun loadByEmail(email: String): Stored<USER>?

    /** Creates a new user during sign-up. Providers call this for the given email/displayName. */
    suspend fun createForSignup(params: CreateUserForSignupParams): Stored<USER>

    /** Serializes the given user for the wire (e.g. the sign-in response payload). */
    suspend fun serialize(user: Stored<USER>): JsonObject
}
