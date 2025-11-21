package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.MinimalTestRealm
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.ultra.vault.Stored
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class GithubSsoAuthSpec : FreeSpec() {

    init {
        "Factory" - {

            "invoke" - {
                "should create a new instance with the given parameters" {
                    val config = AppConfig.of()
                    val subject = GithubSsoAuth.Factory(config)

                    val result = subject(
                        githubClientId = "my-client-id",
                        githubClientSecret = "my-client-secret",
                        capabilities = setOf(
                            AuthProviderModel.Capability.SignIn,
                            AuthProviderModel.Capability.SignUp,
                        )
                    )

                    result.githubClientId shouldBe "my-client-id"
                    result.githubClientSecret shouldBe "my-client-secret"
                    result.capabilities shouldBe setOf(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )
                }
            }

            "fromAppConfig" - {
                "should return null when GITHUB_SSO_CLIENT_ID is not configured" {
                    val config = AppConfig.of(
                        keys = mapOf(
                            GithubSsoAuth.GITHUB_SSO_CLIENT_SECRET to "my-client-secret"
                        )
                    )
                    val subject = GithubSsoAuth.Factory(config)

                    val result = subject.fromAppConfig(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )

                    result.shouldBeNull()
                }

                "should return null when GITHUB_SSO_CLIENT_SECRET is not configured" {
                    val config = AppConfig.of(
                        keys = mapOf(
                            GithubSsoAuth.GITHUB_SSO_CLIENT_ID to "my-client-id"
                        )
                    )
                    val subject = GithubSsoAuth.Factory(config)

                    val result = subject.fromAppConfig(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )

                    result.shouldBeNull()
                }

                "should create an instance when GITHUB_SSO_CLIENT_ID and GITHUB_SSO_CLIENT_SECRET are configured" {
                    val config = AppConfig.of(
                        keys = mapOf(
                            GithubSsoAuth.GITHUB_SSO_CLIENT_ID to "my-client-id",
                            GithubSsoAuth.GITHUB_SSO_CLIENT_SECRET to "my-client-secret"
                        )
                    )

                    val subject = GithubSsoAuth.Factory(config)

                    val result = subject.fromAppConfig(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )

                    result.shouldNotBeNull()
                    result.githubClientId shouldBe "my-client-id"
                    result.githubClientSecret shouldBe "my-client-secret"
                    result.capabilities shouldBe setOf(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )
                }
            }
        }

        "asApiModel" - {
            "should return a correct API model" {
                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    githubClientId = "test-client-id",
                    githubClientSecret = "test-client-secret",
                    remoteClient = lazy { error("Should not be used in this test") }
                )

                val result = subject.asApiModel()

                result.id shouldBe "github-sso"
                result.type shouldBe AuthProviderModel.TYPE_GITHUB
                result.capabilities shouldBe setOf(AuthProviderModel.Capability.SignIn)

                result.config["client-id"]?.jsonPrimitive?.content shouldBe "test-client-id"
            }
        }

        "signIn" - {
            "should throw InvalidCredentials when request is not OAuth" {
                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
                    remoteClient = lazy { error("Should not be called") }
                )

                // A request that is not OAuth
                val request = AuthSignInRequest.EmailAndPassword("test", "", "")

                val realm = MinimalTestRealm()

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when getting the access token fails" {
                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject? {
                        code shouldBe "some-token"
                        return null
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject {
                        error("Should not be called")
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignInRequest.OAuth(provider = subject.id, token = "some-token")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when getting the user fails" {
                val ghAccessToken = buildJsonObject { put("access_token", "gh-access-token") }

                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject {
                        code shouldBe "some-token"
                        return ghAccessToken
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject? {
                        access shouldBe ghAccessToken
                        return null
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignInRequest.OAuth(provider = subject.id, token = "some-token")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when the user has no email" {
                val ghAccessToken = buildJsonObject { put("access_token", "gh-access-token") }
                val ghUser = buildJsonObject { put("id", "123") } // No email

                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject {
                        return ghAccessToken
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject {
                        return ghUser
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
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
                val ghAccessToken = buildJsonObject { put("access_token", "gh-access-token") }
                val ghUser = buildJsonObject { put("email", "user@example.com") }

                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject {
                        return ghAccessToken
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject {
                        return ghUser
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
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
                val ghAccessToken = buildJsonObject { put("access_token", "gh-access-token") }
                val ghUser = buildJsonObject { put("email", "user@example.com") }

                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject {
                        return ghAccessToken
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject {
                        return ghUser
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
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
                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
                    remoteClient = lazy { error("Should not be called") }
                )

                // A request that is not OAuth
                val request = AuthSignUpRequest.EmailAndPassword("test", "", "", "")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }

                error.message shouldBe "Invalid sign-up request for GitHub"
            }

            "should throw InvalidCredentials when getting the access token fails" {
                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject? {
                        code shouldBe "some-token"
                        return null
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject {
                        error("Should not be called")
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when getting the user fails" {
                val ghAccessToken = buildJsonObject { put("access_token", "gh-access-token") }

                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject {
                        return ghAccessToken
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject? {
                        access shouldBe ghAccessToken
                        return null
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
                    remoteClient = lazy { remoteClient }
                )

                val request = AuthSignUpRequest.OAuth(provider = subject.id, token = "some-token")

                val realm = MinimalTestRealm()
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when the user has no email" {
                val ghAccessToken = buildJsonObject { put("access_token", "gh-access-token") }
                val ghUser = buildJsonObject { put("id", "123") } // No email

                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject {
                        return ghAccessToken
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject {
                        return ghUser
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
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
                val ghAccessToken = buildJsonObject { put("access_token", "gh-access-token") }
                val ghUser = buildJsonObject {
                    put("email", "user@example.com")
                    put("name", "User Name")
                }

                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject {
                        return ghAccessToken
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject {
                        return ghUser
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
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
                val ghAccessToken = buildJsonObject { put("access_token", "gh-access-token") }
                val ghUser = buildJsonObject {
                    put("email", "user@example.com")
                    put("name", "User Name")
                }

                // Remote client mock
                val remoteClient = object : GithubSsoAuth.RemoteClient {
                    override suspend fun getAccessToken(code: String): JsonObject {
                        return ghAccessToken
                    }

                    override suspend fun getUser(access: JsonObject): JsonObject {
                        return ghUser
                    }
                }

                val subject = GithubSsoAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    githubClientId = "irrelevant",
                    githubClientSecret = "irrelevant",
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
