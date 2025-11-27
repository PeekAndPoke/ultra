package de.peekandpoke.funktor.auth.provider

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import com.google.api.client.json.gson.GsonFactory
import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.security.GeneralSecurityException

class GoogleSsoAuth(
    override val capabilities: Set<AuthProviderModel.Capability>,
    val clientId: String,
    remoteClient: Lazy<RemoteClient>,
) : AuthProvider {

    companion object {
        const val ID = "google-sso"
        const val GOOGLE_SSO_CLIENT_ID = "GOOGLE_SSO_CLIENT_ID"
        const val GOOGLE_SSO_CLIENT_SECRET = "GOOGLE_SSO_CLIENT_SECRET"
    }

    /**
     * Factory for creating instances of the GoogleSsoAuth provider.
     */
    class Factory(
        private val config: AppConfig,
        private val log: Log,
    ) {
        /**
         * Creates a new instance of the GoogleSsoAuth provider.
         */
        operator fun invoke(
            capabilities: Set<AuthProviderModel.Capability>,
            clientId: String,
            remoteClient: Lazy<RemoteClient> = lazy { RemoteClient(clientId) },
        ) = GoogleSsoAuth(
            clientId = clientId,
            capabilities = capabilities,
            remoteClient = remoteClient,
        )

        /**
         * Creates a new instance of the GoogleSsoAuth provider.
         *
         * Looks for the following keys in the config:
         * - GOOGLE_SSO_CLIENT_ID
         * - GOOGLE_SSO_CLIENT_SECRET
         *
         * When the keys are found, a new instance of the GoogleSsoAuth provider is created.
         * Otherwise, null is returned.
         */
        fun fromAppConfig(vararg capabilities: AuthProviderModel.Capability): GoogleSsoAuth? =
            fromAppConfig(capabilities = capabilities.toSet())

        /**
         * Creates a new instance of the GoogleSsoAuth provider.
         *
         * Looks for the following keys in the config:
         * - GOOGLE_SSO_CLIENT_ID
         * - GOOGLE_SSO_CLIENT_SECRET
         *
         * When the keys are found, a new instance of the GoogleSsoAuth provider is created.
         * Otherwise, null is returned.
         */
        fun fromAppConfig(capabilities: Set<AuthProviderModel.Capability>): GoogleSsoAuth? {
            val clientId = config.getKeyOrNull(GOOGLE_SSO_CLIENT_ID)

            if (clientId == null) {
                log.warning(
                    "Could not find '$GOOGLE_SSO_CLIENT_ID' in the keys config. " +
                            "Skipping Google SSO provider."
                )
                return null
            }

            return invoke(clientId = clientId, capabilities = capabilities)
        }
    }

    /**
     * The remote client talking to the Google API
     */
    interface RemoteClient {
        companion object {
            operator fun invoke(clientId: String): RemoteClient = DefaultRemoteClient(clientId)
        }

        /**
         * Verifies the given id token.
         */
        @Throws(GeneralSecurityException::class, IOException::class)
        suspend fun verifyIdToken(token: String): GoogleIdToken?
    }

    /**
     * Default implementation of the remote client
     */
    private class DefaultRemoteClient(private val clientId: String) : RemoteClient {

        private val tokenVerifier by lazy {
            GoogleIdTokenVerifier
                .Builder(ApacheHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(listOf(clientId))
                .build()
        }

        @Throws(GeneralSecurityException::class, IOException::class)
        override suspend fun verifyIdToken(token: String): GoogleIdToken? {
            return tokenVerifier.verify(token)
        }
    }

    /** The remote client talking to the Google API */
    private val remoteClient: RemoteClient by remoteClient

    /** Provider ID */
    override val id: String = ID

    /**
     * @{inheritDoc}
     */
    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            type = AuthProviderModel.TYPE_GOOGLE,
            capabilities = capabilities,
            config = buildJsonObject {
                put("client-id", clientId)
            },
        )
    }

    /**
     * @{inheritDoc}
     */
    override suspend fun <USER> signIn(
        realm: AuthRealm<USER>, request: AuthSignInRequest,
    ): Stored<USER> {

        val typed = (request as? AuthSignInRequest.OAuth)
            ?: throw AuthError.invalidRequest()

        val idToken: GoogleIdToken = try {
            remoteClient.verifyIdToken(typed.token) ?: throw AuthError.invalidCredentials()
        } catch (e: GeneralSecurityException) {
            throw AuthError.invalidCredentials(e)
        }

        val payload = idToken.payload

        return realm.loadUserByEmail(payload.email)
            ?: throw AuthError.invalidCredentials()
    }

    /**
     * @{inheritDoc}
     */
    override suspend fun <USER> signUp(
        realm: AuthRealm<USER>, request: AuthSignUpRequest,
    ): AuthProvider.SignUpResult<USER> {

        val typed = (request as? AuthSignUpRequest.OAuth)
            ?: throw AuthError("Invalid sign-up request for Google")

        val idToken: GoogleIdToken = try {
            remoteClient.verifyIdToken(typed.token) ?: throw AuthError.invalidCredentials()
        } catch (e: GeneralSecurityException) {
            throw AuthError.invalidCredentials(e)
        }

        val payload = idToken.payload
        val email = payload.email ?: throw AuthError.invalidCredentials()
        val displayName = (payload["name"] as? String)

        val existing = realm.loadUserByEmail(email)
        val user = existing ?: realm.createUserForSignup(
            AuthRealm.CreateUserForSignupParams.of(
                email = email,
                displayName = displayName,
            )
        )

        return AuthProvider.SignUpResult(
            user = user,
            requiresActivation = false,
        )
    }
}
