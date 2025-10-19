package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthRealmModel
import de.peekandpoke.funktor.auth.model.AuthRecoveryRequest
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpResponse
import de.peekandpoke.funktor.auth.model.AuthUpdateRequest
import de.peekandpoke.funktor.auth.model.AuthUpdateResponse
import de.peekandpoke.funktor.auth.model.PasswordPolicy
import de.peekandpoke.funktor.auth.provider.AuthProvider
import de.peekandpoke.funktor.auth.provider.supportsSignIn
import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.JsonObject

interface AuthRealm<USER> {

    interface Messaging<USER> {
        suspend fun sendPasswordChangedEmail(user: Stored<USER>): EmailResult

//        suspend fun sendPasswordResetEmil()
    }

    /** Unique id of the realm */
    val id: String

    /** Auth providers for this realm */
    val providers: List<AuthProvider>

    /** User messaging */
    val messaging: Messaging<USER>

    /** The password policy for this realm */
    val passwordPolicy: PasswordPolicy get() = PasswordPolicy.default

    /** Loads a user by its id. */
    suspend fun loadUserById(id: String): Stored<USER>?

    /** Loads a user by its email. */
    suspend fun loadUserByEmail(email: String): Stored<USER>?

    /** Generates a JWT for the given user */
    suspend fun generateJwt(user: Stored<USER>): AuthSignInResponse.Token

    /** Loads the user email from the given user */
    suspend fun getUserEmail(user: Stored<USER>): String

    /** Serializes the given user */
    suspend fun serializeUser(user: Stored<USER>): JsonObject

    /**
     * Creates a new user during sign-up. Providers call this to create a user for the given email/displayName.
     */
    suspend fun createUserForSignup(email: String, displayName: String?): Stored<USER>

    /**
     * Signs in a user. Providers should check their SignIn capability internally.
     */
    suspend fun signIn(request: AuthSignInRequest): AuthSignInResponse {
        val provider = providers.firstOrNull { it.id == request.provider }
            ?: throw AuthError("Provider not found: ${request.provider}")

        if (provider.supportsSignIn().not()) {
            throw AuthError("Provider does not support sign in: ${request.provider}")
        }

        val user = provider.signIn<USER>(realm = this, request = request)

        val response = user.toSignInResponse()

        return response
    }

    suspend fun update(request: AuthUpdateRequest): AuthUpdateResponse {
        val provider = providers.firstOrNull { it.id == request.provider }
            ?: throw AuthError("Provider not found: ${request.provider}")

        val user = loadUserById(request.userId)
            ?: throw AuthError("User not found: ${request.userId}")

        val result = provider.update(realm = this, user = user, request = request)

        return result
    }

    suspend fun recover(request: AuthRecoveryRequest): AuthRecoveryResponse {
        val provider = providers.firstOrNull { it.id == request.provider }
            ?: throw AuthError("Provider not found: ${request.provider}")

        val result = provider.recover(realm = this, request = request)

        return result
    }

    /**
     * Signs up a new user. Providers should check their SignUp capability internally.
     */
    suspend fun signUp(request: AuthSignUpRequest): AuthSignUpResponse {
        val provider = providers.firstOrNull { it.id == request.provider }
            ?: throw AuthError("Provider not found: ${request.provider}")

        if (AuthProviderModel.Capability.SignUp !in provider.capabilities) {
            throw AuthError("Provider does not support sign up: ${request.provider}")
        }

        val result = provider.signUp(realm = this, request = request)

        val response = AuthSignUpResponse(
            signIn = result.user.toSignInResponse(),
            requiresActivation = result.requiresActivation,
        )

        return response
    }

    suspend fun Stored<USER>.toSignInResponse() = AuthSignInResponse(
        token = generateJwt(this),
        realm = asApiModel(),
        user = serializeUser(this),
    )

    fun asApiModel(): AuthRealmModel = AuthRealmModel(
        id = id,
        providers = providers.map { it.asApiModel() },
        passwordPolicy = passwordPolicy,
    )
}
