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
import de.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.GeneralSecurityException
import java.util.*

class GoogleSsoAuth(
    override val capabilities: Set<AuthProviderModel.Capability>,
    val googleClientId: String,
) : AuthProvider {

    companion object {
        const val ID = "google-sso"
        const val GOOGLE_SSO_CLIENT_ID = "GOOGLE_SSO_CLIENT_ID"
        const val GOOGLE_SSO_CLIENT_SECRET = "GOOGLE_SSO_CLIENT_SECRET"
    }

    class Factory {
        operator fun invoke(
            googleClientId: String,
            capabilities: Set<AuthProviderModel.Capability> = setOf(AuthProviderModel.Capability.SignIn),
        ) = GoogleSsoAuth(
            googleClientId = googleClientId,
            capabilities = capabilities,
        )
    }

    private val verifier by lazy {
        GoogleIdTokenVerifier
            .Builder(ApacheHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build()
    }

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
                put("client-id", googleClientId)
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

        val idToken: GoogleIdToken = try {
            verifier.verify(typed.token)
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
            verifier.verify(typed.token)
        } catch (e: GeneralSecurityException) {
            throw AuthError.invalidCredentials(e)
        }

        val payload = idToken.payload
        val email = payload.email ?: throw AuthError.invalidCredentials()
        val displayName = (payload["name"] as? String)

        val existing = realm.loadUserByEmail(email)
        val user = existing ?: realm.createUserForSignup(email = email, displayName = displayName)

        return AuthProvider.SignUpResult(
            user = user,
            requiresActivation = false,
        )
    }
}
