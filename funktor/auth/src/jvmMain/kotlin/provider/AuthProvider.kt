package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthRecoveryRequest
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.AuthUpdateRequest
import de.peekandpoke.funktor.auth.model.AuthUpdateResponse
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
    suspend fun <USER> signIn(realm: AuthRealm<USER>, request: AuthSignInRequest): Stored<USER> {
        throw AuthError.notSupported()
    }

    /**
     * Updates specific things about the authentication setup of the user
     */
    suspend fun <USER> update(
        realm: AuthRealm<USER>,
        user: Stored<USER>,
        request: AuthUpdateRequest,
    ): AuthUpdateResponse {
        return AuthUpdateResponse(
            success = false,
        )
    }

    /**
     * Account recovery
     */
    suspend fun <USER> recover(realm: AuthRealm<USER>, request: AuthRecoveryRequest): AuthRecoveryResponse {
        return AuthRecoveryResponse(
            success = false,
        )
    }

    /**
     * Sign up a new account for the given request. Providers should check their SignUp capability internally.
     */
    suspend fun <USER> signUp(realm: AuthRealm<USER>, request: AuthSignUpRequest): SignUpResult<USER> {
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
