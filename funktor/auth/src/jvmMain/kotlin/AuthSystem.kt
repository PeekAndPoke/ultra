package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.model.AuthActivateRequest
import de.peekandpoke.funktor.auth.model.AuthActivateResponse
import de.peekandpoke.funktor.auth.model.AuthRecoveryRequest
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpResponse
import de.peekandpoke.funktor.auth.model.AuthUpdateRequest
import de.peekandpoke.funktor.auth.model.AuthUpdateResponse
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
    private val realms: List<AuthRealm<Any>>,
) {
    class Deps(
        val config: AppConfig,
        kronos: Lazy<Kronos>,
        messaging: Lazy<MessagingServices>,
        jwtGenerator: Lazy<JwtGenerator>,
        storage: Lazy<Storage>,
        passwordHasher: Lazy<PasswordHasher>,
        random: Lazy<AuthRandom>,
    ) {
        val kronos by kronos
        val messaging by messaging
        val jwtGenerator by jwtGenerator
        val storage by storage
        val passwordHasher by passwordHasher
        val random by random
    }

    class Storage(
        authRecords: Lazy<AuthRecordStorage>,
    ) {
        val authRecords by authRecords
    }

    init {
        val duplicatedRealms = realms.groupBy { it.id }
            .filter { (id, realms) -> realms.size > 1 }
            .map { (id, realms) -> id }

        if (duplicatedRealms.isNotEmpty()) {
            throw error("Found duplicated authentication realms: $duplicatedRealms")
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

    /** Get realm by [realm] or throw [AuthError] if not present */
    suspend fun signIn(realm: String, request: AuthSignInRequest): AuthSignInResponse {
        return getRealm(realm).signIn(request)
    }

    /** Get realm by [realm] or throw [AuthError] if not present */
    suspend fun update(realm: String, request: AuthUpdateRequest): AuthUpdateResponse {
        return getRealm(realm).update(request)
    }

    /** Get realm by [realm] or throw [AuthError] if not present */
    suspend fun recover(realm: String, request: AuthRecoveryRequest): AuthRecoveryResponse {
        return getRealm(realm).recoverPassword(request)
    }

    /** Get realm by [realm] or throw [AuthError] if not present */
    suspend fun signUp(realm: String, request: AuthSignUpRequest): AuthSignUpResponse {
        return getRealm(realm).signUp(request)
    }

    /** Activate a user account by [realm] and [request] */
    suspend fun activate(realm: String, request: AuthActivateRequest): AuthActivateResponse {
        // Activation not yet implemented
        // TODO: implement me
        return AuthActivateResponse(success = false)
    }
}
