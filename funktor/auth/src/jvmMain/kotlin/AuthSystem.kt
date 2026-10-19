package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpResponse
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.messaging.MessagingServices
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.security.jwt.JwtGenerator
import io.peekandpoke.ultra.security.password.PasswordHasher
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserId

/**
 * AuthSystem manages and provides access to multiple authentication realms.
 *
 * @property realms A list of authentication realms that this system handles.
 *                  Realms must have unique identifiers to avoid duplication.
 */
class AuthSystem(
    realms: Lazy<List<AuthRealm<*>>>,
    deps: Lazy<Deps>,
) {
    interface Deps {
        val config: AppConfig
        val kronos: Kronos
        val messaging: MessagingServices
        val jwtGenerator: JwtGenerator
        val storage: Storage
        val passwordHasher: PasswordHasher
        val log: Log
        val random: AuthRandom
    }

    class DefaultDeps(
        override val config: AppConfig,
        kronos: Lazy<Kronos>,
        messaging: Lazy<MessagingServices>,
        jwtGenerator: Lazy<JwtGenerator>,
        storage: Lazy<Storage>,
        passwordHasher: Lazy<PasswordHasher>,
        log: Lazy<Log>,
        random: Lazy<AuthRandom>,
    ) : Deps {
        override val kronos by kronos
        override val messaging by messaging
        override val jwtGenerator by jwtGenerator
        override val storage by storage
        override val passwordHasher by passwordHasher
        override val log by log
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
    fun getRealmOrNull(realm: RealmId): AuthRealm<*>? {
        return realms.firstOrNull { it.id == realm }
    }

    /** Get realm by [realm] or throw [AuthError] if not present */
    fun getRealm(realm: RealmId): AuthRealm<*> {
        return getRealmOrNull(realm) ?: throw AuthError("Realm not found: $realm")
    }

    /** Sign up a new user by [realm] and [request] */
    suspend fun signUp(realm: RealmId, request: AuthSignUpRequest): AuthSignUpResponse {
        return getRealm(realm).signUp(request)
    }

    /** Activate a user account by [realm] and [request] */
    suspend fun activate(realm: RealmId, request: AuthActivateAccountRequest): AuthActivateAccountResponse {
        return getRealm(realm).activate(request)
    }

    /** Send a fresh activation mail for [realm] and [request] */
    suspend fun resendActivation(realm: RealmId, request: AuthResendActivationRequest): AuthResendActivationResponse {
        return getRealm(realm).resendActivation(request)
    }

    /** Retire a never-activated account by [realm] and [request] ("I did not sign up") */
    suspend fun cancelSignUp(realm: RealmId, request: AuthCancelSignUpRequest): AuthCancelSignUpResponse {
        return getRealm(realm).cancelSignUp(request)
    }

    /**
     * Retires the pending accounts of every realm that outlived `RealmTokenConfig.pendingAccountLifetime`,
     * up to [limitPerRealm] each; returns how many. Meant for a periodic worker — expiry is also enforced
     * where it matters (activation, resend, sign-up), so this is garbage collection, not the guard.
     */
    suspend fun retireExpiredPendingAccounts(limitPerRealm: Int = 100): Int {
        return realms.sumOf { realm ->
            try {
                realm.retireExpiredPendingAccounts(limit = limitPerRealm)
            } catch (e: Exception) {
                deps.log.error("Retiring expired pending accounts failed in realm ${realm.id}", e)
                0
            }
        }
    }

    /** Sign in a user by [realm] and [request] */
    suspend fun signIn(realm: RealmId, request: AuthSignInRequest): AuthSignInResponse {
        return getRealm(realm).signIn(request)
    }

    /** Completes an org-selection sign-in for [realm] using the selection token and chosen org. */
    suspend fun selectOrg(realm: RealmId, selectionToken: String, orgId: OrgId): AuthSignInResponse {
        return getRealm(realm).selectOrg(selectionToken, orgId)
    }

    /** Refresh the token for the user with the given [userId] in the given [realm] */
    suspend fun refreshToken(
        realm: RealmId, userId: UserId, expectedUserType: String?, currentOrgId: OrgId?,
    ): AuthSignInResponse {
        return getRealm(realm).refreshToken(userId, expectedUserType, currentOrgId)
    }

    /** Set the password of a user by [realm] and [request] */
    suspend fun setPassword(realm: RealmId, request: AuthSetPasswordRequest): AuthSetPasswordResponse {
        return getRealm(realm).setPassword(request)
    }

    /** Initialise password recovery by [realm] and [request] */
    suspend fun recoverAccountInitPasswordReset(
        realm: RealmId, request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset {
        return getRealm(realm).recoverAccountInitPasswordReset(request)
    }

    /** Validate password reset token by [realm] and [request] */
    suspend fun recoverAccountValidatePasswordResetToken(
        realm: RealmId, request: AuthRecoverAccountRequest.ValidatePasswordResetToken,
    ): AuthRecoverAccountResponse.ValidatePasswordResetToken {
        return getRealm(realm).recoverAccountValidatePasswordResetToken(request)
    }

    /** Reset password with token by [realm] and [request] */
    suspend fun recoverAccountSetPasswordWithToken(
        realm: RealmId, request: AuthRecoverAccountRequest.SetPasswordWithToken,
    ): AuthRecoverAccountResponse.SetPasswordWithToken {
        return getRealm(realm).recoverAccountSetPasswordWithToken(request)
    }
}
