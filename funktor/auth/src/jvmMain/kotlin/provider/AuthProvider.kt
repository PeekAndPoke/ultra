package io.peekandpoke.funktor.auth.provider

import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpResponse
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
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
     *
     * An SSO-style provider that resolves the user by email MUST pass it through
     * `AuthRealm.resolveSsoAccount`, in [signUp] too — it refuses an address the provider did not verify, and
     * keeps a pending password account from being taken over.
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
     *
     * An SSO provider passes an existing account found by email through `AuthRealm.resolveSsoAccount`, as for
     * [signIn], and creates a new account only for an address the provider verified.
     */
    suspend fun <USER : AuthUser> signUp(
        realm: AuthRealm<USER>, request: AuthSignUpRequest,
    ): SignUpResult<USER> {
        throw AuthError.notSupported()
    }

    /**
     * Consumes an activation token issued by this provider at sign-up, sets the account's first password and
     * activates it; returns the activated user, whom the realm signs in.
     *
     * Answers null for an unknown, used or expired token rather than throwing — the endpoint is anonymous, so
     * an error would tell an attacker which tokens exist. Null too for an account that is not pending: a
     * leftover link must never set the password of an active account.
     */
    suspend fun <USER : AuthUser> activateAccount(
        realm: AuthRealm<USER>, request: AuthActivateAccountRequest,
    ): Stored<USER>? {
        throw AuthError.notSupported()
    }

    /**
     * Retires a never-activated account, authorized by the cancel token mailed at sign-up (or by the
     * activation token, from the activation page's own cancel button).
     *
     * Answers `success = false` rather than throwing for an unusable token, as [activateAccount] answers null.
     */
    suspend fun <USER : AuthUser> cancelSignUp(
        realm: AuthRealm<USER>, request: AuthCancelSignUpRequest,
    ): AuthCancelSignUpResponse {
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
