package io.peekandpoke.funktor.auth.provider

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthUserAdapter
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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
                clientId = clientId.value,
                clientSecret = clientSecret.value,
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

        /**
         * Gets the user's addresses (`GET /user/emails`, needs the `user:email` scope), or null.
         *
         * Defaults to null, which reads as "nothing verified" — the safe answer.
         */
        suspend fun getEmails(access: JsonObject): JsonArray? = null
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

        /** @{inheritDoc} */
        override suspend fun getEmails(access: JsonObject): JsonArray? {

            val tokenType = access["token_type"]?.jsonPrimitive?.content
            val accessToken = access["access_token"]?.jsonPrimitive?.content

            val response = httpClient.get("https://api.github.com/user/emails") {
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "$tokenType $accessToken")
            }

            if (!response.status.isSuccess()) return null

            return response.body<JsonArray?>()
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
    override suspend fun <USER : AuthUser> signIn(
        realm: AuthRealm<USER>, request: AuthSignInRequest,
    ): Stored<USER> {
        val typed = (request as? AuthSignInRequest.OAuth)
            ?: throw AuthError.invalidCredentials()

        val ghAccessToken = remoteClient.getAccessToken(typed.token)
            ?: throw AuthError.invalidCredentials()

        val ghUser = remoteClient.getUser(ghAccessToken)
            ?: throw AuthError.invalidCredentials()

        // GAP CLOSED: the provider's email is third-party input and was used raw against a
        // case-sensitive lookup, so a mixed-case address failed to match an existing account.
        val email = EmailAddress.parseOrNull((ghUser["email"] as? JsonPrimitive)?.contentOrNull)
            ?: throw AuthError.invalidCredentials()

        val user = realm.users.loadByEmail(email)
            ?: throw AuthError.invalidCredentials()

        return realm.resolveSsoAccount(user, provider = id) {
            isVerifiedPrimary(email, remoteClient.getEmails(ghAccessToken))
        } ?: throw AuthError.invalidCredentials()
    }

    /**
     * GitHub's verified claim: [email] is the account's PRIMARY address and GitHub has verified it. The
     * profile's `email` field alone proves nothing — GitHub does not require it to be verified.
     */
    private fun isVerifiedPrimary(email: EmailAddress, emails: JsonArray?): Boolean =
        emails.orEmpty().any { entry ->
            val obj = entry as? JsonObject ?: return@any false

            EmailAddress.parseOrNull((obj["email"] as? JsonPrimitive)?.contentOrNull) == email &&
                    (obj["primary"] as? JsonPrimitive)?.booleanOrNull == true &&
                    (obj["verified"] as? JsonPrimitive)?.booleanOrNull == true
        }

    /**
     * @{inheritDoc}
     */
    override suspend fun <USER : AuthUser> signUp(
        realm: AuthRealm<USER>, request: AuthSignUpRequest,
    ): AuthProvider.SignUpResult<USER> {

        val typed = (request as? AuthSignUpRequest.OAuth)
            ?: throw AuthError("Invalid sign-up request for GitHub")

        val ghAccessToken = remoteClient.getAccessToken(typed.token)
            ?: throw AuthError.invalidCredentials()

        val ghUser = remoteClient.getUser(ghAccessToken)
            ?: throw AuthError.invalidCredentials()

        // GAP CLOSED: raw here meant the existence check below could MISS and create a duplicate
        // user, because creation canonicalizes but the lookup did not.
        val email = EmailAddress.parseOrNull((ghUser["email"] as? JsonPrimitive)?.contentOrNull)
            ?: throw AuthError.invalidCredentials()

        val name = ghUser["name"]?.jsonPrimitive?.content

        // The lookup is deliberately lax about format — an address stored before validation
        // existed must stay matchable, or its owner can never sign in again.
        val existing = realm.users.loadByEmail(email)
        // Creating, however, requires a well-formed address.
        if (existing == null && !email.isValidFormat) throw AuthError.invalidCredentials()

        val user = existing
            ?.let {
                realm.resolveSsoAccount(it, provider = id) {
                    isVerifiedPrimary(email, remoteClient.getEmails(ghAccessToken))
                }
            }
            ?: realm.users.createForSignup(
                AuthUserAdapter.CreateUserForSignupParams.of(
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
