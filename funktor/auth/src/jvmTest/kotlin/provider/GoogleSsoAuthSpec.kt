package io.peekandpoke.funktor.auth.provider

import io.peekandpoke.ultra.common.model.Redacted
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.json.webtoken.JsonWebSignature
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.MinimalTestRealm
import io.peekandpoke.funktor.auth.MinimalTestUser
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.jsonPrimitive
import java.security.GeneralSecurityException
import java.io.IOException
import java.util.Base64

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
                            GoogleSsoAuth.GOOGLE_SSO_CLIENT_ID to Redacted("my-client-id")
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
                    email = "  User@EXAMPLE.com  "
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
                        email shouldBe EmailAddress("user@example.com")
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
                    email = "  User@EXAMPLE.com  "
                    emailVerified = true
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

                val storedUser = Stored(_id = "repo/user-id", value = MinimalTestUser())

                val request = AuthSignInRequest.OAuth(provider = subject.id, token = "some-token")

                // A realm that returns the user
                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { email ->
                        email shouldBe EmailAddress("user@example.com")
                        // USER FOUND
                        storedUser
                    },
                    onLoadUserById = { storedUser }, // re-read after the marker read (ULTRA-00288)
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
                val request = AuthSignUpRequest.EmailAndPassword("test", "")

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

            "a verifier that fails for another reason (a token it cannot parse, Google's keys unreachable) refuses, no 500" {
                // ULTRA-00288 item 3. Only GeneralSecurityException was caught; these escaped as a 500.
                listOf(IllegalArgumentException("not a JWT"), IOException("certs unreachable")).forEach { failure ->
                    val subject = GoogleSsoAuth(
                        capabilities = setOf(AuthProviderModel.Capability.SignIn, AuthProviderModel.Capability.SignUp),
                        clientId = "irrelevant",
                        remoteClient = lazy {
                            object : GoogleSsoAuth.RemoteClient {
                                override suspend fun verifyIdToken(token: String): GoogleIdToken? = throw failure
                            }
                        },
                    )

                    withClue(failure::class.simpleName) {
                        shouldThrow<AuthError> {
                            subject.signIn(MinimalTestRealm(), AuthSignInRequest.OAuth(provider = subject.id, token = "t"))
                        }.message shouldBe "Invalid credentials"
                        shouldThrow<AuthError> {
                            subject.signUp(MinimalTestRealm(), AuthSignUpRequest.OAuth(provider = subject.id, token = "t"))
                        }.message shouldBe "Invalid credentials"
                    }
                }
            }

            "the REAL verifier, fed tokens built to break it, refuses each one, no 500" {
                // No network: each fails before the keys are fetched. A numeric `aud` made the payload check throw a
                // ClassCastException, which no narrow catch covered.
                fun b64(json: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
                val claims = """"iss":"accounts.google.com","exp":9999999999"""
                val tokens = listOf(
                    "x",
                    "a.b.c",
                    "!!!.!!!.!!!",
                    b64("""{"alg":"RS256"}""") + "." + b64("""{$claims,"aud":1}""") + ".AA",
                    b64("""{"alg":"ES256"}""") + "." + b64("""{$claims,"aud":"cid"}""") + ".AA",
                )
                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    clientId = "cid",
                    remoteClient = lazy { GoogleSsoAuth.RemoteClient("cid") },
                )

                tokens.forEach { token ->
                    withClue(token) {
                        shouldThrow<AuthError> {
                            subject.signIn(MinimalTestRealm(), AuthSignInRequest.OAuth(provider = subject.id, token = token))
                        }.message shouldBe "Invalid credentials"
                    }
                }
            }

            "a sign-up whose user cannot be created (a concurrent sign-up won the unique index) refuses, no 500" {
                val payload = GoogleIdToken.Payload().apply {
                    email = "new@example.com"
                    emailVerified = true
                }
                val idToken = GoogleIdToken(JsonWebSignature.Header(), payload, byteArrayOf(), byteArrayOf())
                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    clientId = "irrelevant",
                    remoteClient = lazy {
                        object : GoogleSsoAuth.RemoteClient {
                            override suspend fun verifyIdToken(token: String): GoogleIdToken = idToken
                        }
                    },
                )
                // Not an Exception: a Karango user repository fails with Throwables. The adapter's own AuthError (a realm
                // without sign-up) keeps its message.
                listOf(
                    object : Throwable("unique constraint violated") {} to "User already exists",
                    AuthError.notSupported() to "Not supported",
                ).forEach { (failure, answer) ->
                    val realm = MinimalTestRealm(onLoadUserByEmail = { null }, onCreateUserForSignup = { throw failure })

                    withClue(failure.message) {
                        shouldThrow<AuthError> {
                            subject.signUp(realm, AuthSignUpRequest.OAuth(provider = subject.id, token = "t"))
                        }.message shouldBe answer
                    }
                }
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
                    email = "  User@EXAMPLE.com  "
                    emailVerified = true
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

                val newUser = Stored(_id = "new-user-id", value = MinimalTestUser())

                // A realm that creates a new user
                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { email ->
                        email shouldBe EmailAddress("user@example.com")
                        null // User does not exist
                    },
                    onCreateUserForSignup = { params ->
                        params.email shouldBe EmailAddress("user@example.com")
                        params.displayName shouldBe "User Name"
                        newUser
                    }
                )

                val request = AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token")

                val result = subject.signUp(realm, request)

                result.user shouldBe newUser
                result.requiresActivation shouldBe false
            }

            "a missing, blank or refused profile name becomes the address's local part; a good one is kept, trimmed" {
                // ULTRA-00293: the profile name is someone else's text; it meets the same rule as the activation page.
                val cases = mapOf<Any?, String>(
                    null to "user",
                    "   " to "user",
                    "Ada${Char(0x202E)}evol" to "user",
                    "Call${Char(0x0A)}support" to "user",
                    "  Ada Lovelace " to "Ada Lovelace",
                )

                for ((name, expected) in cases) {
                    val tokenPayload = GoogleIdToken.Payload().apply {
                        email = "User@EXAMPLE.com"
                        emailVerified = true
                        if (name != null) set("name", name)
                    }
                    val idToken = GoogleIdToken(JsonWebSignature.Header(), tokenPayload, byteArrayOf(), byteArrayOf())
                    val subject = GoogleSsoAuth(
                        capabilities = setOf(AuthProviderModel.Capability.SignUp),
                        clientId = "irrelevant",
                        remoteClient = lazy {
                            object : GoogleSsoAuth.RemoteClient {
                                override suspend fun verifyIdToken(token: String): GoogleIdToken = idToken
                            }
                        },
                    )
                    var written: String? = null
                    val realm = MinimalTestRealm(
                        onLoadUserByEmail = { null },
                        onCreateUserForSignup = { params ->
                            written = params.displayName
                            Stored(_id = "new-user-id", value = MinimalTestUser())
                        },
                    )

                    subject.signUp(realm, AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token"))

                    withClue("name ${name?.toString()?.map { it.code }}") { written shouldBe expected }
                }
            }

            "a local part the rule refuses as too long is cut to MAX_LENGTH" {
                // The address format allows a local part of 150 chars; the name rule allows 100.
                val local = "a".repeat(150)
                val tokenPayload = GoogleIdToken.Payload().apply {
                    email = "$local@example.com"
                    emailVerified = true
                }
                val idToken = GoogleIdToken(JsonWebSignature.Header(), tokenPayload, byteArrayOf(), byteArrayOf())
                val subject = GoogleSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    clientId = "irrelevant",
                    remoteClient = lazy {
                        object : GoogleSsoAuth.RemoteClient {
                            override suspend fun verifyIdToken(token: String): GoogleIdToken = idToken
                        }
                    },
                )
                var written: String? = null
                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { null },
                    onCreateUserForSignup = { params ->
                        written = params.displayName
                        Stored(_id = "new-user-id", value = MinimalTestUser())
                    },
                )

                subject.signUp(realm, AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token"))

                written shouldBe "a".repeat(100)
            }

            "should return the existing user when the user already exists" {
                // A valid token payload for the tests below
                val tokenPayload = GoogleIdToken.Payload().apply {
                    email = "  User@EXAMPLE.com  "
                    emailVerified = true
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

                val existingUser = Stored(_id = "existing-user-id", value = MinimalTestUser())

                // A realm that finds an existing user
                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { email ->
                        email shouldBe EmailAddress("user@example.com")
                        existingUser // User exists
                    },
                    onLoadUserById = { existingUser }, // re-read after the marker read (ULTRA-00288)
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
