package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import de.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.ultra.vault.Stored

interface AuthProvider {

    data class SignUpResult<USER>(
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
    suspend fun <USER> signIn(
        realm: AuthRealm<USER>, request: AuthSignInRequest,
    ): Stored<USER> {
        throw AuthError.notSupported()
    }

    /**
     * Updates specific things about the authentication setup of the user
     */
    suspend fun <USER> setPassword(
        realm: AuthRealm<USER>, request: AuthSetPasswordRequest,
    ): AuthSetPasswordResponse {
        throw AuthError.notSupported()
    }

    /**
     * Sign up a new account for the given request. Providers should check their SignUp capability internally.
     */
    suspend fun <USER> signUp(
        realm: AuthRealm<USER>, request: AuthSignUpRequest,
    ): SignUpResult<USER> {
        throw AuthError.notSupported()
    }

    /**
     * Init account password recovery
     */
    suspend fun <USER> recoverAccountInitPasswordReset(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset {
        throw AuthError.notSupported()
    }

    /**
     * Validate token for password reset
     */
    suspend fun <USER> recoverAccountValidatePasswordResetToken(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.ValidatePasswordResetToken,
    ): AuthRecoverAccountResponse.ValidatePasswordResetToken {
        throw AuthError.notSupported()
    }

    /**
     * Recover account by setting a new password
     */
    suspend fun <USER> recoverAccountSetPasswordWithToken(
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
