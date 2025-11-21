package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.MinimalTestDeps
import de.peekandpoke.funktor.auth.MinimalTestRealm
import de.peekandpoke.funktor.auth.TestMessaging
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import de.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.PasswordPolicy
import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.log.NullLog
import de.peekandpoke.ultra.vault.Stored
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class EmailAndPasswordAuthSpec : FreeSpec() {

    private open class TestServices(
        val onHashPassword: (String) -> String = { error("hashPassword not implemented") },
        val onCheckPassword: (String, String) -> Boolean = { _, _ -> error("checkPassword not implemented") },
        val onFindLatestPasswordRecord: suspend (String, String) -> Stored<AuthRecord.Password>? = { _, _ -> error("findLatestPasswordRecord not implemented") },
        val onCreateAuthRecord: suspend (create: () -> AuthRecord) -> Stored<AuthRecord> = { error("createAuthRecord not implemented") },
        val onGenerateToken: (Int) -> String = { error("generateToken not implemented") },
        val onInstantNow: () -> MpInstant = { error("instantNow not implemented") },
        val onFindPasswordRecoveryToken: suspend (String, String) -> Stored<AuthRecord.PasswordRecoveryToken>? =
            { _, _ -> error("findPasswordRecoveryToken not implemented") },
    ) : EmailAndPasswordAuth.Services {
        override fun hashPassword(password: String): String = onHashPassword(password)
        override fun checkPassword(plaintext: String, hash: String): Boolean = onCheckPassword(plaintext, hash)
        override suspend fun findLatestPasswordRecord(realm: String, owner: String): Stored<AuthRecord.Password>? =
            onFindLatestPasswordRecord(realm, owner)

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T : AuthRecord> createAuthRecord(record: () -> T): Stored<T> =
            onCreateAuthRecord(record) as Stored<T>

        override fun generateRandomBase64Token(length: Int): String = onGenerateToken(length)

        override fun instantNow(): MpInstant = onInstantNow()

        override suspend fun findPasswordRecoveryToken(
            realm: String,
            token: String,
        ): Stored<AuthRecord.PasswordRecoveryToken>? = onFindPasswordRecoveryToken(realm, token)
    }

    init {
        "Factory" - {
            "invoke" - {
                "should create a new instance with the given parameters" {

                    val deps = lazy { MinimalTestDeps() }
                    val urls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c")
                    val subject = EmailAndPasswordAuth.Factory(deps, NullLog)

                    val result = subject(
                        frontendUrls = urls,
                        capabilities = setOf(
                            AuthProviderModel.Capability.SignIn,
                            AuthProviderModel.Capability.SignUp,
                        )
                    )

                    result.id shouldBe "email-password"
                    result.capabilities shouldContainExactly setOf(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )
                }
            }
        }

        "asApiModel" - {
            "should return a correct API model" {

                val deps = lazy { MinimalTestDeps() }
                val urls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c")
                val factory = EmailAndPasswordAuth.Factory(deps, NullLog)
                val subject = factory(urls, setOf(AuthProviderModel.Capability.SignIn))

                val result = subject.asApiModel()

                result.id shouldBe "email-password"
                result.type shouldBe AuthProviderModel.TYPE_EMAIL_PASSWORD
                result.capabilities shouldContainExactly setOf(AuthProviderModel.Capability.SignIn)
                result.config.isEmpty() shouldBe true
            }
        }

        "signIn" - {

            "should throw InvalidCredentials when request is not EmailAndPassword" {
                val deps = lazy { MinimalTestDeps() }
                val urls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c")
                val factory = EmailAndPasswordAuth.Factory(deps, NullLog)
                val subject = factory(urls, setOf(AuthProviderModel.Capability.SignIn))

                // A request that is not EmailAndPassword
                val request = AuthSignInRequest.OAuth("test", "")

                val realm = MinimalTestRealm()

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid request"
            }

            "should throw InvalidCredentials when email is blank" {
                val deps = lazy { MinimalTestDeps() }
                val urls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c")
                val factory = EmailAndPasswordAuth.Factory(deps, NullLog)
                val subject = factory(urls, setOf(AuthProviderModel.Capability.SignIn))

                val request = AuthSignInRequest.EmailAndPassword(
                    provider = subject.id,
                    email = " ",
                    password = "password"
                )

                val realm = MinimalTestRealm()

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when password is blank" {
                val deps = lazy { MinimalTestDeps() }
                val urls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c")
                val factory = EmailAndPasswordAuth.Factory(deps, NullLog)
                val subject = factory(urls, setOf(AuthProviderModel.Capability.SignIn))

                val request = AuthSignInRequest.EmailAndPassword(
                    provider = subject.id,
                    email = "user@example.com",
                    password = " "
                )

                val realm = MinimalTestRealm()

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when user is not found by email" {
                val deps = lazy { MinimalTestDeps() }
                val urls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c")
                val factory = EmailAndPasswordAuth.Factory(deps, NullLog)
                val subject = factory(urls, setOf(AuthProviderModel.Capability.SignIn))

                val request = AuthSignInRequest.EmailAndPassword(
                    provider = subject.id,
                    email = "user@example.com",
                    password = "password"
                )

                val realm = MinimalTestRealm(
                    onLoadUserByEmail = {
                        it shouldBe "user@example.com"
                        null // user not found
                    }
                )

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when password is wrong" {

                val storedUser = Stored(_id = "user-id", value = Any())

                val services = lazy<EmailAndPasswordAuth.Services> {
                    TestServices(
                        onCheckPassword = { _, _ -> false }, // password does not match
                        onFindLatestPasswordRecord = { _, _ ->
                            Stored(
                                _id = "password-id",
                                value = AuthRecord.Password(
                                    realm = "test-realm",
                                    ownerId = storedUser._id,
                                    token = "hashed-password"
                                )
                            )
                        }
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    log = NullLog,
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    services = services
                )

                val request = AuthSignInRequest.EmailAndPassword(
                    provider = subject.id,
                    email = "user@example.com",
                    password = "wrong-password"
                )

                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { storedUser } // user is found
                )

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should return the user on successful sign in" {

                val storedUser = Stored(_id = "user-id", value = Any())

                val services = lazy<EmailAndPasswordAuth.Services> {
                    TestServices(
                        onCheckPassword = { _, _ -> true }, // password matches
                        onFindLatestPasswordRecord = { _, _ ->
                            Stored(
                                _id = "password-id",
                                value = AuthRecord.Password(
                                    realm = "test-realm",
                                    ownerId = storedUser._id,
                                    token = "hashed-password"
                                )
                            )
                        }
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    log = NullLog,
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    services = services
                )

                val request = AuthSignInRequest.EmailAndPassword(
                    provider = subject.id,
                    email = "user@example.com",
                    password = "correct-password"
                )

                val realm = MinimalTestRealm(
                    onLoadUserByEmail = { storedUser }
                )

                val result = subject.signIn(realm, request)

                result shouldBe storedUser
            }
        }

        "signUp" - {

            "should throw when request is not for email and password" {
                // Setup
                val services = lazy { TestServices() }
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )

                // A sign-up request that is not for email and password
                val request = AuthSignUpRequest.OAuth("test", "")
                val realm = MinimalTestRealm()

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }

                error.message shouldBe "Invalid request"
            }

            "should throw when email is blank" {
                // Setup
                val services = lazy { TestServices() }
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSignUpRequest.EmailAndPassword(
                    provider = subject.id,
                    email = " ",
                    password = "password",
                    displayName = "Test User"
                )
                val realm = MinimalTestRealm()

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }
                error.message shouldBe "Invalid request"
            }

            "should throw when email is not a valid email" {
                // Setup
                val services = lazy { TestServices() }
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSignUpRequest.EmailAndPassword(
                    provider = subject.id,
                    email = "this-is-not-an-email",
                    password = "password",
                    displayName = "Test User"
                )
                val realm = MinimalTestRealm()

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }
                error.message shouldBe "Invalid request"
            }

            "should throw when password is weak" {
                // Setup
                val services = lazy { TestServices() }
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSignUpRequest.EmailAndPassword(
                    provider = subject.id,
                    email = "test@example.com",
                    password = "weak",
                    displayName = "Test User"
                )
                val realm = MinimalTestRealm(
                    passwordPolicy = PasswordPolicy.default
                )

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }
                error.message shouldBe "Weak password"
            }

            "should throw when user already exists" {
                // Setup
                val services = lazy { TestServices() }
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSignUpRequest.EmailAndPassword(
                    provider = subject.id,
                    email = "test@example.com",
                    password = "A-valid-password-123!",
                    displayName = "Test User"
                )
                val realm = MinimalTestRealm(
                    passwordPolicy = PasswordPolicy.default,
                    onLoadUserByEmail = {
                        it shouldBe "test@example.com"
                        Stored(_id = "existing-user", value = Any())
                    }
                )

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }
                error.message shouldBe "User already exists"
            }

            "should sign up user successfully" {
                // Setup
                val storedUser = Stored(_id = "user-id", value = Any())
                val password = "A-valid-password-123!"
                val hashedPassword = "hashed-password"
                val email = "test@example.com"
                val displayName = "Test User"

                var createdAuthRecord: Stored<AuthRecord>? = null

                val services = lazy {
                    TestServices(
                        onHashPassword = {
                            it shouldBe password
                            hashedPassword
                        },
                        onCreateAuthRecord = { create ->
                            val record = create() as AuthRecord.Password
                            Stored(_id = "new-password-record", value = record).also {
                                @Suppress("UNCHECKED_CAST")
                                createdAuthRecord = it as Stored<AuthRecord>
                            }
                        }
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )

                val request = AuthSignUpRequest.EmailAndPassword(
                    provider = subject.id,
                    email = email,
                    password = password,
                    displayName = displayName
                )

                val realm = MinimalTestRealm(
                    passwordPolicy = PasswordPolicy.default,
                    onLoadUserByEmail = { null },
                    onCreateUserForSignup = { params ->
                        params.email shouldBe email
                        params.displayName shouldBe displayName
                        storedUser
                    }
                )

                // Execute
                val result = subject.signUp(realm, request)

                // Verify
                result.user shouldBe storedUser
                result.requiresActivation shouldBe true

                val createdPassword = createdAuthRecord!!.value as AuthRecord.Password
                createdPassword.ownerId shouldBe storedUser._id
                createdPassword.token shouldBe hashedPassword
                createdPassword.realm shouldBe realm.id
            }
        }

        "setPassword" - {

            "should throw weakPassword when new password does not meet policy" {
                // Setup
                val services = lazy { TestServices() }
                val subject = EmailAndPasswordAuth(
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSetPasswordRequest(
                    provider = subject.id,
                    userId = "user-id",
                    newPassword = "weak"
                )
                val realm = MinimalTestRealm(
                    passwordPolicy = PasswordPolicy.default
                )

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.setPassword(realm, request)
                }
                error.message shouldBe "Weak password"
            }

            "should throw userNotFound when user does not exist" {
                // Setup
                val services = lazy { TestServices() }
                val subject = EmailAndPasswordAuth(
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )

                val request = AuthSetPasswordRequest(
                    provider = subject.id,
                    userId = "user-id",
                    newPassword = "A-valid-password-123!"
                )

                val realm = MinimalTestRealm(
                    onLoadUserById = {
                        it shouldBe "user-id"
                        null // User not found
                    }
                )

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.setPassword(realm, request)
                }
                error.message shouldBe "User 'user-id' not found"
            }

            "should set the password successfully" {
                // Setup
                val storedUser = Stored(_id = "user-id", value = Any())
                var passwordEmailSent = false
                val newPassword = "Strong-password-123!"
                val hashedPassword = "hashed-new-password"

                val services = lazy {
                    TestServices(
                        onHashPassword = {
                            it shouldBe newPassword
                            hashedPassword // return
                        },
                        onCreateAuthRecord = { create ->
                            val record = create() as AuthRecord.Password
                            record.realm shouldBe "test-realm"
                            record.ownerId shouldBe storedUser._id
                            record.token shouldBe hashedPassword

                            Stored(_id = "new-password-record", value = record)
                        }
                    )
                }
                val subject = EmailAndPasswordAuth(
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSetPasswordRequest(
                    provider = subject.id,
                    userId = storedUser._id,
                    newPassword = newPassword
                )
                val realm = MinimalTestRealm(
                    onLoadUserById = {
                        it shouldBe storedUser._id
                        storedUser
                    },
                    getMessaging = {
                        TestMessaging(
                            onSendPasswordChangedEmail = {
                                it shouldBe storedUser
                                passwordEmailSent = true
                                EmailResult.ofMessageId(messageId = "message-id")
                            }
                        )
                    },
                    onGetUserEmail = { "user@example.com" }
                )

                // Execute
                val response = subject.setPassword(realm, request)

                // Verify
                response shouldBe AuthSetPasswordResponse(success = true)
                passwordEmailSent shouldBe true
            }
        }
    }
}
