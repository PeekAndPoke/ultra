package io.peekandpoke.funktor.auth.provider

import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.ultra.vault.Stored

interface AuthProvider {

    data class SignUpResult<USER : AuthUser>(
        val user: Stored<USER>,
        val requiresActivation: Boolean,
    )

    /**
     * Unique id of the provider with the realm
     */
    val id: String

    /**
     * Capabilities of the provider
     */
    val capabilities: Set<AuthProviderModel.Capability>

    /**
     * Tries to log in the user for the given [request].
     *
     * The user will be returned when it is found, and the request was validated successfully.
     *
     * Otherwise [AuthError] will be thrown.
     */
    suspend fun <USER : AuthUser> signIn(
        realm: AuthRealm<USER>, request: AuthSignInRequest,
    ): Stored<USER> {
        throw AuthError.notSupported()
    }

    /**
     * Updates specific things about the authentication setup of the user
     */
    suspend fun <USER : AuthUser> setPassword(
        realm: AuthRealm<USER>, request: AuthSetPasswordRequest,
    ): AuthSetPasswordResponse {
        throw AuthError.notSupported()
    }

    /**
     * Sign up a new account for the given request. Providers should check their SignUp capability internally.
     */
    suspend fun <USER : AuthUser> signUp(
        realm: AuthRealm<USER>, request: AuthSignUpRequest,
    ): SignUpResult<USER> {
        throw AuthError.notSupported()
    }

    /**
     * Consumes an activation token issued by this provider at sign-up and activates the account.
     *
     * Answers `success = false` for an unknown or expired token rather than throwing — the endpoint is
     * anonymous, so an error would tell an attacker which tokens exist.
     */
    suspend fun <USER : AuthUser> activateAccount(
        realm: AuthRealm<USER>, request: AuthActivateAccountRequest,
    ): AuthActivateAccountResponse {
        throw AuthError.notSupported()
    }

    /**
     * Issues a fresh activation token and mails it again.
     *
     * Answers the SAME neutral response in every case — unknown address, already-activated account,
     * inside the cooldown window — because the endpoint is anonymous.
     */
    suspend fun <USER : AuthUser> resendActivation(
        realm: AuthRealm<USER>, request: AuthResendActivationRequest,
    ): AuthResendActivationResponse {
        throw AuthError.notSupported()
    }

    /**
     * Init account password recovery
     */
    suspend fun <USER : AuthUser> recoverAccountInitPasswordReset(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset {
        throw AuthError.notSupported()
    }

    /**
     * Validate token for password reset
     */
    suspend fun <USER : AuthUser> recoverAccountValidatePasswordResetToken(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.ValidatePasswordResetToken,
    ): AuthRecoverAccountResponse.ValidatePasswordResetToken {
        throw AuthError.notSupported()
    }

    /**
     * Recover account by setting a new password
     */
    suspend fun <USER : AuthUser> recoverAccountSetPasswordWithToken(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.SetPasswordWithToken,
    ): AuthRecoverAccountResponse.SetPasswordWithToken {
        throw AuthError.notSupported()
    }

    /**
     * As api model
     */
    fun asApiModel(): AuthProviderModel
}

/**
 * Checks if the provider has the given capability
 */
fun AuthProvider.hasCapability(capability: AuthProviderModel.Capability): Boolean = capability in capabilities

/**
 * Checks if the provider supports sign-in
 */
fun AuthProvider.supportsSignIn(): Boolean = hasCapability(AuthProviderModel.Capability.SignIn)

/**
 * Checks if the provider supports sign-up
 */
fun AuthProvider.supportsSignUp(): Boolean = hasCapability(AuthProviderModel.Capability.SignUp)
