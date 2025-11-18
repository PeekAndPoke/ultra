package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
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
    val githubClientId: String,
    val githubClientSecret: String,
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

    class Factory {
        operator fun invoke(
            githubClientId: String,
            githubClientSecret: String,
            capabilities: Set<AuthProviderModel.Capability> = setOf(AuthProviderModel.Capability.SignIn),
        ) = GithubSsoAuth(
            githubClientId = githubClientId,
            githubClientSecret = githubClientSecret,
            capabilities = capabilities,
        )
    }

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
                put("client-id", githubClientId)
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

        // see https://medium.com/@r.sadatshokouhi/implementing-sso-in-react-with-github-oauth2-4d8dbf02e607

        val ghAccessToken = getAccessToken(typed.token)
            ?: throw AuthError.invalidCredentials()

        val ghUser = getUser(ghAccessToken)
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

        val ghAccessToken = getAccessToken(typed.token)
            ?: throw AuthError.invalidCredentials()

        val ghUser = getUser(ghAccessToken)
            ?: throw AuthError.invalidCredentials()

        val email = ghUser["email"]?.jsonPrimitive?.content
            ?: throw AuthError.invalidCredentials()

        val name = ghUser["name"]?.jsonPrimitive?.content

        val existing = realm.loadUserByEmail(email)
        val user = existing ?: realm.createUserForSignup(email = email, displayName = name)

        return AuthProvider.SignUpResult(
            user = user,
            requiresActivation = false,
        )
    }

    private suspend fun getAccessToken(code: String): JsonObject? {
        val response = httpClient.post("https://github.com/login/oauth/access_token") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("client_id", githubClientId)
                put("client_secret", githubClientSecret)
                put("code", code)
            })
        }

        if (!response.status.isSuccess()) return null

        return response.body<JsonObject?>()
    }

    private suspend fun getUser(access: JsonObject): JsonObject? {

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
