package de.peekandpoke.funktor.auth.provider

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.json.webtoken.JsonWebSignature
import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.MinimalTestRealm
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.ultra.log.NullLog
import de.peekandpoke.ultra.vault.Stored
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonPrimitive
import java.security.GeneralSecurityException


class GoogleSsoAuthSpec : FreeSpec() {

    init {
        "Factory" - {

            "invoke" - {
                "should create a new instance with the given parameters" {
                    val config = AppConfig.of()
                    val subject = GoogleSsoAuth.Factory(config, NullLog)

                    val result = subject(
                        clientId = "my-client-id",
                        capabilities = setOf(
                            AuthProviderModel.Capability.SignIn,
                            AuthProviderModel.Capability.SignUp,
                        )
                    )

                    result.clientId shouldBe "my-client-id"
                    result.capabilities shouldBe setOf(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )
                }
            }

            "fromAppConfig" - {
                "should return null when GOOGLE_SSO_CLIENT_ID is not configured" {
                    val config = AppConfig.of()
                    val subject = GoogleSsoAuth.Factory(config, NullLog)

                    val result = subject.fromAppConfig(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )

                    result.shouldBeNull()
                }

                "should create an instance when GOOGLE_SSO_CLIENT_ID is configured" {
                    val config = AppConfig.of(
                        keys = mapOf(
                            GoogleSsoAuth.GOOGLE_SSO_CLIENT_ID to "my-client-id"
                        )
                    )

                    val subject = GoogleSsoAuth.Factory(config, NullLog)

                    val result = subject.fromAppConfig(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )

                    result.shouldNotBeNull()
                    result.clientId shouldBe "my-client-id"
                    result.capabilities shouldBe setOf(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )
                }
            }
        }

        "asApiModel" - {
            "should return a correct API model" {
                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    clientId = "test-client-id",
                    remoteClient = lazy { error("Should not be used in this test") }
                )

                val result = subject.asApiModel()

                result.id shouldBe "google-sso"
                result.type shouldBe AuthProviderModel.TYPE_GOOGLE
                result.capabilities shouldBe setOf(AuthProviderModel.Capability.SignIn)

                result.config["client-id"]?.jsonPrimitive?.content shouldBe "test-client-id"
            }
        }

        "signIn" - {
            "should throw InvalidCredentials when request is not OAuth" {
                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    clientId = "irrelevant",
                    remoteClient = lazy { error("Should not be called") }
                )

                // A request that is not OAuth
                val request = AuthSignInRequest.EmailAndPassword("test", "", "")

                val realm = MinimalTestRealm()

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid request"
            }

            "should throw InvalidCredentials when token verification fails" {
                // Remote client mock
                val remoteClient = object : GoogleSsoAuth.RemoteClient {
                    override suspend fun verifyIdToken(token: String): GoogleIdToken {
                        throw GeneralSecurityException("Token is invalid!")
                    }
                }

                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    clientId = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignInRequest.OAuth(provider = subject.id, token = "some-token")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when user is not found by email" {
                // A valid token payload for the tests below
                val tokenPayload = GoogleIdToken.Payload().apply {
                    email = "user@example.com"
                }
                val idToken = GoogleIdToken(JsonWebSignature.Header(), tokenPayload, byteArrayOf(), byteArrayOf())

                // A verifier that returns a valid token
                // Remote client mock
                val remoteClient = object : GoogleSsoAuth.RemoteClient {
                    override suspend fun verifyIdToken(token: String): GoogleIdToken {
                        return idToken
                    }
                }

                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    clientId = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignInRequest.OAuth(provider = subject.id, token = "some-token")

                // A realm that returns 'null' for the user
                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { email ->
                        email shouldBe "user@example.com"
                        // NOT FOUND
                        null
                    }
                )

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should return the user when sign in is successful" {
                val tokenPayload = GoogleIdToken.Payload().apply {
                    email = "user@example.com"
                }
                val idToken = GoogleIdToken(JsonWebSignature.Header(), tokenPayload, byteArrayOf(), byteArrayOf())

                // Remote client mock
                val remoteClient = object : GoogleSsoAuth.RemoteClient {
                    override suspend fun verifyIdToken(token: String): GoogleIdToken {
                        return idToken
                    }
                }

                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    clientId = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val storedUser = Stored(_id = "repo/user-id", value = Any())

                val request = AuthSignInRequest.OAuth(provider = subject.id, token = "some-token")

                // A realm that returns the user
                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { email ->
                        email shouldBe "user@example.com"
                        // USER FOUND
                        storedUser
                    }
                )

                val result = subject.signIn(realm, request)

                result shouldBe storedUser
            }

        }

        "signUp" - {
            "should throw an error when request is not OAuth" {
                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    clientId = "irrelevant",
                    remoteClient = lazy { error("Should not be called") }
                )

                // A request that is not OAuth
                val request = AuthSignUpRequest.EmailAndPassword("test", "", "", "")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }

                error.message shouldBe "Invalid sign-up request for Google"
            }

            "should throw InvalidCredentials when token verification fails" {
                // Remote client mock
                val remoteClient = object : GoogleSsoAuth.RemoteClient {
                    override suspend fun verifyIdToken(token: String): GoogleIdToken {
                        throw GeneralSecurityException("Token is invalid!")
                    }
                }

                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    clientId = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "Remote client can return null when token verification fails" {
                // Remote client mock
                val remoteClient = object : GoogleSsoAuth.RemoteClient {
                    override suspend fun verifyIdToken(token: String): GoogleIdToken? {
                        return null
                    }
                }

                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    clientId = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when token payload has no email" {
                val payloadWithoutEmail = GoogleIdToken.Payload().apply {
                    set("name", "User Name")
                }
                val idTokenWithoutEmail =
                    GoogleIdToken(JsonWebSignature.Header(), payloadWithoutEmail, byteArrayOf(), byteArrayOf())

                // Remote client mock
                val remoteClient = object : GoogleSsoAuth.RemoteClient {
                    override suspend fun verifyIdToken(token: String): GoogleIdToken {
                        return idTokenWithoutEmail
                    }
                }

                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    clientId = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should sign-up a new user when the user does not exist yet" {
                // A valid token payload for the tests below
                val tokenPayload = GoogleIdToken.Payload().apply {
                    email = "user@example.com"
                    set("name", "User Name")
                }
                val idToken = GoogleIdToken(JsonWebSignature.Header(), tokenPayload, byteArrayOf(), byteArrayOf())

                // Remote client mock
                val remoteClient = object : GoogleSsoAuth.RemoteClient {
                    override suspend fun verifyIdToken(token: String): GoogleIdToken {
                        return idToken
                    }
                }

                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    clientId = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val newUser = Stored(_id = "new-user-id", value = Any())

                // A realm that creates a new user
                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { email ->
                        email shouldBe "user@example.com"
                        null // User does not exist
                    },
                    onCreateUserForSignup = { params ->
                        params.email shouldBe "user@example.com"
                        params.displayName shouldBe "User Name"
                        newUser
                    }
                )

                val request = AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token")

                val result = subject.signUp(realm, request)

                result.user shouldBe newUser
                result.requiresActivation shouldBe false
            }

            "should return the existing user when the user already exists" {
                // A valid token payload for the tests below
                val tokenPayload = GoogleIdToken.Payload().apply {
                    email = "user@example.com"
                    set("name", "User Name")
                }
                val idToken = GoogleIdToken(JsonWebSignature.Header(), tokenPayload, byteArrayOf(), byteArrayOf())

                // Remote client mock
                val remoteClient = object : GoogleSsoAuth.RemoteClient {
                    override suspend fun verifyIdToken(token: String): GoogleIdToken {
                        return idToken
                    }
                }

                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    clientId = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val existingUser = Stored(_id = "existing-user-id", value = Any())

                // A realm that finds an existing user
                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { email ->
                        email shouldBe "user@example.com"
                        existingUser // User exists
                    },
                    onCreateUserForSignup = { _ ->
                        error("Should not be called")
                    }
                )

                val request = AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token")

                val result = subject.signUp(realm, request)

                result.user shouldBe existingUser
                result.requiresActivation shouldBe false
            }
        }
    }
}
