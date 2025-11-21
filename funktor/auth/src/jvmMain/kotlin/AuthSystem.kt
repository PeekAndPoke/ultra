package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import de.peekandpoke.funktor.auth.model.AuthActivateActivateResponse
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import de.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpResponse
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.messaging.MessagingServices
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.security.password.PasswordHasher

/**
 * AuthSystem manages and provides access to multiple authentication realms.
 *
 * @property realms A list of authentication realms that this system handles.
 *                  Realms must have unique identifiers to avoid duplication.
 */
class AuthSystem(
    realms: Lazy<List<AuthRealm<Any>>>,
    deps: Lazy<Deps>,
) {
    interface Deps {
        val config: AppConfig
        val kronos: Kronos
        val messaging: MessagingServices
        val jwtGenerator: JwtGenerator
        val storage: Storage
        val passwordHasher: PasswordHasher
        val random: AuthRandom
    }

    class DefaultDeps(
        override val config: AppConfig,
        kronos: Lazy<Kronos>,
        messaging: Lazy<MessagingServices>,
        jwtGenerator: Lazy<JwtGenerator>,
        storage: Lazy<Storage>,
        passwordHasher: Lazy<PasswordHasher>,
        random: Lazy<AuthRandom>,
    ) : Deps {
        override val kronos by kronos
        override val messaging by messaging
        override val jwtGenerator by jwtGenerator
        override val storage by storage
        override val passwordHasher by passwordHasher
        override val random by random
    }

    class Storage(
        authRecords: Lazy<AuthRecordStorage>,
    ) {
        val authRecords by authRecords
    }

    val realms by realms
    val deps by deps

    /** Validate that all realms
     *
     * Check that all realms are configured properly.
     * And that there are no duplicated realms by their id.
     */
    fun validateRealms() {
        val duplicatedRealms = realms.groupBy { it.id }
            .filter { (_, realms) -> realms.size > 1 }
            .map { (id, _) -> id }

        if (duplicatedRealms.isNotEmpty()) {
            throw AuthError("Found duplicated authentication realms: $duplicatedRealms")
        }
    }

    /** Get realm by [realm] or null if not present */
    fun getRealmOrNull(realm: String): AuthRealm<*>? {
        return realms.firstOrNull { it.id == realm }
    }

    /** Get realm by [realm] or throw [AuthError] if not present */
    fun getRealm(realm: String): AuthRealm<*> {
        return getRealmOrNull(realm) ?: throw AuthError("Realm not found: $realm")
    }

    /** Sign up a new user by [realm] and [request] */
    suspend fun signUp(realm: String, request: AuthSignUpRequest): AuthSignUpResponse {
        return getRealm(realm).signUp(request)
    }

    /** Activate a user account by [realm] and [request] */
    suspend fun activate(realm: String, request: AuthActivateAccountRequest): AuthActivateActivateResponse {
        // Activation not yet implemented
        // TODO: implement me
        return AuthActivateActivateResponse(success = false)
    }

    /** Sign in a user by [realm] and [request] */
    suspend fun signIn(realm: String, request: AuthSignInRequest): AuthSignInResponse {
        return getRealm(realm).signIn(request)
    }

    /** Set the password of a user by [realm] and [request] */
    suspend fun setPassword(realm: String, request: AuthSetPasswordRequest): AuthSetPasswordResponse {
        return getRealm(realm).setPassword(request)
    }

    /** Initialise password recovery by [realm] and [request] */
    suspend fun recoverAccountInitPasswordReset(
        realm: String, request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset {
        return getRealm(realm).recoverAccountInitPasswordReset(request)
    }

    /** Validate password reset token by [realm] and [request] */
    suspend fun recoverAccountValidatePasswordResetToken(
        realm: String, request: AuthRecoverAccountRequest.ValidatePasswordResetToken,
    ): AuthRecoverAccountResponse.ValidatePasswordResetToken {
        return getRealm(realm).recoverAccountValidatePasswordResetToken(request)
    }

    /** Reset password with token by [realm] and [request] */
    suspend fun recoverAccountSetPasswordWithToken(
        realm: String, request: AuthRecoverAccountRequest.SetPasswordWithToken,
    ): AuthRecoverAccountResponse.SetPasswordWithToken {
        return getRealm(realm).recoverAccountSetPasswordWithToken(request)
    }
}
