package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.vault.Stored
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class GithubSsoAuth(
    override val capabilities: Set<AuthProviderModel.Capability>,
    val clientId: String,
    val clientSecret: String,
    remoteClient: Lazy<RemoteClient> = lazy {
        RemoteClient(clientId = clientId, clientSecret = clientSecret)
    },
) : AuthProvider {

    companion object {
        const val ID = "github-sso"
        const val GITHUB_SSO_CLIENT_ID = "GITHUB_SSO_CLIENT_ID"
        const val GITHUB_SSO_CLIENT_SECRET = "GITHUB_SSO_CLIENT_SECRET"

        private val httpClient = HttpClient {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    /**
     * Factory for creating a new instance of the GithubSsoAuth provider.
     */
    class Factory(
        private val config: AppConfig,
        private val log: Log,
    ) {
        operator fun invoke(
            clientId: String,
            clientSecret: String,
            capabilities: Set<AuthProviderModel.Capability>,
        ) = GithubSsoAuth(
            clientId = clientId,
            clientSecret = clientSecret,
            capabilities = capabilities,
        )

        fun fromAppConfig(vararg capabilities: AuthProviderModel.Capability): GithubSsoAuth? =
            fromAppConfig(capabilities = capabilities.toSet())

        fun fromAppConfig(capabilities: Set<AuthProviderModel.Capability>): GithubSsoAuth? {
            val clientId = config.getKeyOrNull(GITHUB_SSO_CLIENT_ID)
            val clientSecret = config.getKeyOrNull(GITHUB_SSO_CLIENT_SECRET)

            if (clientId == null || clientSecret == null) {
                log.warning(
                    "Could not find '$GITHUB_SSO_CLIENT_ID' or '$GITHUB_SSO_CLIENT_SECRET' in the keys config. " +
                            "Skipping Github SSO provider."
                )
                return null
            }

            return invoke(
                clientId = clientId,
                clientSecret = clientSecret,
                capabilities = capabilities,
            )
        }
    }

    /** Remote client talking to the Github API */
    interface RemoteClient {
        companion object {
            /** Creates a new instance of the remote client */
            operator fun invoke(clientId: String, clientSecret: String): RemoteClient =
                DefaultRemoteClient(clientId, clientSecret)
        }

        /** Gets the access token for the given [code] */
        suspend fun getAccessToken(code: String): JsonObject?

        /** Gets the user for the given [access] token */
        suspend fun getUser(access: JsonObject): JsonObject?
    }

    /** Default implementation of the remote client */
    private class DefaultRemoteClient(private val clientId: String, private val clientSecret: String) : RemoteClient {

        /** @{inheritDoc} */
        override suspend fun getAccessToken(code: String): JsonObject? {
            // see https://medium.com/@r.sadatshokouhi/implementing-sso-in-react-with-github-oauth2-4d8dbf02e607

            val url = "https://github.com/login/oauth/access_token"

            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("client_id", clientId)
                    put("client_secret", clientSecret)
                    put("code", code)
                })
            }

            if (!response.status.isSuccess()) return null

            return response.body<JsonObject?>()
        }

        /** @{inheritDoc} */
        override suspend fun getUser(access: JsonObject): JsonObject? {

            val tokenType = access["token_type"]?.jsonPrimitive?.content
            val accessToken = access["access_token"]?.jsonPrimitive?.content

            val response = httpClient.get("https://api.github.com/user") {
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "$tokenType $accessToken")
            }

            if (!response.status.isSuccess()) return null

            return response.body<JsonObject?>()
        }

    }

    /** Remote client talking to the Github API */
    private val remoteClient by lazy { remoteClient.value }

    /** Provider ID */
    override val id: String = ID

    /**
     * @{inheritDoc}
     */
    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            type = AuthProviderModel.TYPE_GITHUB,
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
            ?: throw AuthError.invalidCredentials()

        val ghAccessToken = remoteClient.getAccessToken(typed.token)
            ?: throw AuthError.invalidCredentials()

        val ghUser = remoteClient.getUser(ghAccessToken)
            ?: throw AuthError.invalidCredentials()

        val email = ghUser["email"]?.jsonPrimitive?.content
            ?: throw AuthError.invalidCredentials()

        return realm.loadUserByEmail(email)
            ?: throw AuthError.invalidCredentials()
    }

    /**
     * @{inheritDoc}
     */
    override suspend fun <USER> signUp(
        realm: AuthRealm<USER>, request: AuthSignUpRequest,
    ): AuthProvider.SignUpResult<USER> {

        val typed = (request as? AuthSignUpRequest.OAuth)
            ?: throw AuthError("Invalid sign-up request for GitHub")

        val ghAccessToken = remoteClient.getAccessToken(typed.token)
            ?: throw AuthError.invalidCredentials()

        val ghUser = remoteClient.getUser(ghAccessToken)
            ?: throw AuthError.invalidCredentials()

        val email = ghUser["email"]?.jsonPrimitive?.content
            ?: throw AuthError.invalidCredentials()

        val name = ghUser["name"]?.jsonPrimitive?.content

        val existing = realm.loadUserByEmail(email)
        val user = existing ?: realm.createUserForSignup(
            AuthRealm.CreateUserForSignupParams.of(
                email = email,
                displayName = name,
            )
        )

        return AuthProvider.SignUpResult(
            user = user,
            requiresActivation = false,
        )
    }
}
