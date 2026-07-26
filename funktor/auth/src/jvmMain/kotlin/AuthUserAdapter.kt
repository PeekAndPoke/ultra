package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.ultra.security.user.EmailAddress
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
        val email: EmailAddress,
        val displayName: String,
    ) {
        companion object {
            /**
             * [email] is ALREADY canonical.
             *
             * There is deliberately NO `String` overload: it would be the throwing one of an
             * otherwise identical-looking pair, and the reflex choice for a caller holding raw
             * input. Spell the conversion at the call site with [EmailAddress.of] /
             * [EmailAddress.parseOrNull] so the failure mode is visible there.
             *
             * NOTE [displayName] defaults to the CANONICAL local part, so an auto-derived name is
             * lowercase (`John.Doe@x.com` → `john.doe`). Pass an explicit [displayName] to preserve
             * the user's casing.
             */
            fun of(email: EmailAddress, displayName: String? = null): CreateUserForSignupParams {
                // THE creation boundary — the one place RFC format is enforced. A lookup deliberately
                // does not check it (an odd-but-stored address must stay matchable), but writing a
                // NEW account with a malformed address is a real defect.
                require(email.isValidFormat) { "Cannot create a user with a malformed email address" }

                return CreateUserForSignupParams(
                    email = email,
                    displayName = displayName?.trim() ?: email.localPart,
                )
            }
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

    /**
     * Loads a user by its email.
     *
     * [EmailAddress] is canonical by construction, so this lookup is case-insensitive by TYPE — the
     * repos' `findByEmail` are case-sensitive exact matches and no longer need callers to remember
     * to normalize. Feed raw user input through [EmailAddress.of] first.
     */
    suspend fun loadByEmail(email: EmailAddress): Stored<USER>?

    /** Creates a new user during sign-up. Providers call this for the given email/displayName. */
    suspend fun createForSignup(params: CreateUserForSignupParams): Stored<USER>

    /** Serializes the given user for the wire (e.g. the sign-in response payload). */
    suspend fun serialize(user: Stored<USER>): JsonObject
}
