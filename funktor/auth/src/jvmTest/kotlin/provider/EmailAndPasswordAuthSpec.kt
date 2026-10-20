package io.peekandpoke.funktor.auth.provider

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import io.peekandpoke.funktor.auth.AccountRetirement
import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.MinimalTestDeps
import io.peekandpoke.funktor.auth.MinimalTestRealm
import io.peekandpoke.funktor.auth.MinimalTestUser
import io.peekandpoke.funktor.auth.NullStorageTestDeps
import io.peekandpoke.funktor.auth.RealmTokenConfig
import io.peekandpoke.funktor.auth.TestMessaging
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Stored
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class EmailAndPasswordAuthSpec : FreeSpec() {

    private open class TestServices(
        val onHashPassword: (String) -> String = { error("hashPassword not implemented") },
        val onCheckPassword: (String, String) -> Boolean = { _, _ -> error("checkPassword not implemented") },
        val onFindLatestPasswordRecord: suspend (RealmId, UserId) -> Stored<AuthRecord.Password>? = { _, _ -> error("findLatestPasswordRecord not implemented") },
        val onCreateAuthRecord: suspend (create: () -> AuthRecord) -> Stored<AuthRecord> = { error("createAuthRecord not implemented") },
        val onGenerateToken: (Int) -> String = { error("generateToken not implemented") },
        val onInstantNow: () -> MpInstant = { error("instantNow not implemented") },
        val onFindPasswordRecoveryToken: suspend (RealmId, String) -> Stored<AuthRecord.PasswordRecoveryToken>? =
            { _, _ -> error("findPasswordRecoveryToken not implemented") },
        val onFindEmailVerificationToken: suspend (RealmId, String) -> Stored<AuthRecord.EmailVerificationToken>? =
            { _, _ -> error("findEmailVerificationToken not implemented") },
        val onRemoveEmailVerificationTokens: suspend (RealmId, UserId, String?) -> Unit =
            { _, _, _ -> error("removeEmailVerificationTokens not implemented") },
        val onFindPendingActivation: suspend (RealmId, UserId) -> Stored<AuthRecord.PendingActivation>? =
            { _, _ -> error("findPendingActivation not implemented") },
        val onReplacePendingActivation: suspend (Stored<AuthRecord.PendingActivation>) -> Stored<AuthRecord.PendingActivation>? =
            { error("replacePendingActivation not implemented") },
        val onFindSignUpCancelToken: suspend (RealmId, String) -> Stored<AuthRecord.SignUpCancelToken>? =
            { _, _ -> error("findSignUpCancelToken not implemented") },
        val onFindSignUpCancelTokenOf: suspend (RealmId, UserId) -> Stored<AuthRecord.SignUpCancelToken>? =
            { _, _ -> error("findSignUpCancelTokenOf not implemented") },
        val onRemovePendingActivationIfUnclaimed: suspend (String) -> Boolean =
            { error("removePendingActivationIfUnclaimed not implemented") },
        val onRemoveAuthRecord: suspend (String) -> Boolean = { error("removeAuthRecord not implemented") },
    ) : EmailAndPasswordAuth.Services {
        override fun hashPassword(password: String): String = onHashPassword(password)
        override fun checkPassword(plaintext: String, hash: String): Boolean = onCheckPassword(plaintext, hash)
        override suspend fun findLatestPasswordRecord(realm: RealmId, owner: UserId): Stored<AuthRecord.Password>? =
            onFindLatestPasswordRecord(realm, owner)

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T : AuthRecord> createAuthRecord(record: () -> T): Stored<T> =
            onCreateAuthRecord(record) as Stored<T>

        override fun generateRandomBase64Token(length: Int): String = onGenerateToken(length)

        override fun instantNow(): MpInstant = onInstantNow()

        override suspend fun findPasswordRecoveryToken(
            realm: RealmId,
            token: String,
        ): Stored<AuthRecord.PasswordRecoveryToken>? = onFindPasswordRecoveryToken(realm, token)

        override suspend fun findEmailVerificationToken(
            realm: RealmId,
            token: String,
        ): Stored<AuthRecord.EmailVerificationToken>? = onFindEmailVerificationToken(realm, token)

        override suspend fun removeEmailVerificationTokens(realm: RealmId, owner: UserId, exceptId: String?) =
            onRemoveEmailVerificationTokens(realm, owner, exceptId)

        override suspend fun findPendingActivation(
            realm: RealmId,
            owner: UserId,
        ): Stored<AuthRecord.PendingActivation>? = onFindPendingActivation(realm, owner)

        override suspend fun replacePendingActivation(
            marker: Stored<AuthRecord.PendingActivation>,
        ): Stored<AuthRecord.PendingActivation>? = onReplacePendingActivation(marker)

        override suspend fun findSignUpCancelToken(realm: RealmId, token: String) = onFindSignUpCancelToken(realm, token)

        override suspend fun findSignUpCancelTokenOf(realm: RealmId, owner: UserId) =
            onFindSignUpCancelTokenOf(realm, owner)

        override suspend fun removePendingActivationIfUnclaimed(id: String) = onRemovePendingActivationIfUnclaimed(id)

        override suspend fun removeAuthRecord(id: String) = onRemoveAuthRecord(id)
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
                        it shouldBe EmailAddress("user@example.com")
                        null // user not found
                    }
                )

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Invalid credentials"
            }

            "should throw InvalidCredentials when password is wrong" {

                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())

                val services = lazy<EmailAndPasswordAuth.Services> {
                    TestServices(
                        onCheckPassword = { _, _ -> false }, // password does not match
                        onFindLatestPasswordRecord = { _, _ ->
                            Stored(
                                _id = "password-id",
                                value = AuthRecord.Password(
                                    realm = RealmId("test-realm"),
                                    ownerId = UserId(storedUser._id),
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

                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())

                val services = lazy<EmailAndPasswordAuth.Services> {
                    TestServices(
                        onCheckPassword = { _, _ -> true }, // password matches
                        onFindLatestPasswordRecord = { _, _ ->
                            Stored(
                                _id = "password-id",
                                value = AuthRecord.Password(
                                    realm = RealmId("test-realm"),
                                    ownerId = UserId(storedUser._id),
                                    token = "hashed-password"
                                )
                            )
                        },
                        onFindPendingActivation = { _, _ -> null }, // the account is activated
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

            "should refuse sign in while the account is pending activation" {
                // The credentials are CORRECT here. Anything less would pass for the wrong reason —
                // the point is that a valid password is not enough until the address is proven.

                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())

                val services = lazy<EmailAndPasswordAuth.Services> {
                    TestServices(
                        onCheckPassword = { _, _ -> true },
                        onFindLatestPasswordRecord = { _, _ ->
                            Stored(
                                _id = "password-id",
                                value = AuthRecord.Password(
                                    realm = RealmId("test-realm"),
                                    ownerId = UserId(storedUser._id),
                                    token = "hashed-password"
                                )
                            )
                        },
                        onFindPendingActivation = { realm, owner ->
                            owner shouldBe UserId(storedUser._id)
                            Stored(
                                _id = "pending-activation",
                                value = AuthRecord.PendingActivation(realm = realm, ownerId = owner),
                            )
                        },
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

                val error = shouldThrow<AuthError> {
                    subject.signIn(realm, request)
                }

                error.message shouldBe "Account not activated"
            }

            "a RETIRED key never signs in — the account is not even looked up" {
                // `onLoadUserByEmail` keeps its exploding default.
                val retired = AccountRetirement.of(UserId("users/1")).rename(EmailAddress.of("user@example.com"))
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    log = NullLog,
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    services = lazy { TestServices() },
                )

                shouldThrow<AuthError> {
                    subject.signIn(
                        MinimalTestRealm(),
                        AuthSignInRequest.EmailAndPassword(provider = subject.id, email = retired.value, password = "pw"),
                    )
                }.message shouldBe "Invalid credentials"
            }

            "a CLAIMED account refuses sign-in without a resend token, even with the right password" {
                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    log = NullLog,
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    services = lazy {
                        TestServices(
                            onCheckPassword = { _, _ -> true },
                            onFindLatestPasswordRecord = { realm, owner ->
                                Stored(_id = "pw", value = AuthRecord.Password(realm = realm, ownerId = owner, token = "h"))
                            },
                            onFindPendingActivation = { realm, owner ->
                                Stored(
                                    _id = "pending",
                                    value = AuthRecord.PendingActivation(
                                        realm = realm, ownerId = owner,
                                        claimedBy = AuthRecord.PendingActivation.CLAIMED_BY_RETIRE,
                                    ),
                                )
                            },
                        )
                    },
                )

                val error = shouldThrow<AuthError> {
                    subject.signIn(
                        MinimalTestRealm(onLoadUserByEmail = { storedUser }),
                        AuthSignInRequest.EmailAndPassword(provider = subject.id, email = "user@example.com", password = "pw"),
                    )
                }

                error.shouldNotBeInstanceOf<AuthError.AccountNotActivated>()
            }

            "should check the PASSWORD before the pending marker" {
                // The ordering IS the authorization for minting a resend token: `AccountNotActivated`
                // makes the realm hand one out, so it must only ever follow a correct password. The
                // account here IS pending, so a marker check that ran first would answer
                // "Account not activated" to a wrong password.
                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
                var markerChecked = false

                val services = lazy<EmailAndPasswordAuth.Services> {
                    TestServices(
                        onCheckPassword = { _, _ -> false },
                        onFindLatestPasswordRecord = { realm, owner ->
                            Stored(
                                _id = "password-id",
                                value = AuthRecord.Password(realm = realm, ownerId = owner, token = "hashed-password"),
                            )
                        },
                        onFindPendingActivation = { realm, owner ->
                            markerChecked = true
                            Stored(_id = "pending", value = AuthRecord.PendingActivation(realm = realm, ownerId = owner))
                        },
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    log = NullLog,
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    services = services
                )

                val error = shouldThrow<AuthError> {
                    subject.signIn(
                        MinimalTestRealm(onLoadUserByEmail = { storedUser }),
                        AuthSignInRequest.EmailAndPassword(
                            provider = subject.id, email = "user@example.com", password = "wrong-password",
                        ),
                    )
                }

                error.shouldNotBeInstanceOf<AuthError.AccountNotActivated>()
                error.message shouldBe "Invalid credentials"
                markerChecked shouldBe false
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
                        it shouldBe EmailAddress("test@example.com")
                        Stored(_id = "existing-user", value = MinimalTestUser())
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
                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
                val password = "A-valid-password-123!"
                val hashedPassword = "hashed-password"
                val email = "test@example.com"
                val displayName = "Test User"
                val verificationToken = "the-verification-token"
                val now = MpInstant.parse("2026-07-27T10:00:00Z")

                val createdAuthRecords = mutableListOf<AuthRecord>()
                var activationUrl: String? = null
                var cancelUrl: String? = null

                val services = lazy {
                    TestServices(
                        onHashPassword = {
                            it shouldBe password
                            hashedPassword
                        },
                        onCreateAuthRecord = { create ->
                            val record = create()
                            createdAuthRecords.add(record)
                            @Suppress("UNCHECKED_CAST")
                            Stored(_id = "record-${createdAuthRecords.size}", value = record)
                        },
                        onGenerateToken = { verificationToken },
                        onInstantNow = { now },
                        // A brand-new user has no earlier tokens; the shared issue-and-send path calls
                        // this anyway, so the sign-up and resend links cannot drift apart.
                        onRemoveEmailVerificationTokens = { _, _, _ -> },
                        onFindSignUpCancelTokenOf = { _, _ -> null },
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
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
                        params.email shouldBe EmailAddress.of(email)
                        params.displayName shouldBe displayName
                        storedUser
                    },
                    getMessaging = {
                        TestMessaging(
                            onSendAccountActivationEmail = { user, url, cancel ->
                                user shouldBe storedUser
                                activationUrl = url
                                cancelUrl = cancel
                                EmailResult.ofMessageId("message-id")
                            }
                        )
                    },
                )

                // Execute
                val result = subject.signUp(realm, request)

                // Verify
                result.user shouldBe storedUser
                result.requiresActivation shouldBe true

                val createdPassword = createdAuthRecords.filterIsInstance<AuthRecord.Password>().single()
                createdPassword.ownerId shouldBe UserId(storedUser._id)
                createdPassword.token shouldBe hashedPassword
                createdPassword.realm shouldBe realm.id

                // The marker is what actually blocks sign-in, so its absence would make the whole
                // activation flow decorative — assert it exists, and that it is non-expiring. A marker
                // with an expiry would silently activate the account when it lapsed.
                val marker = createdAuthRecords.filterIsInstance<AuthRecord.PendingActivation>().single()
                marker.ownerId shouldBe UserId(storedUser._id)
                marker.realm shouldBe realm.id
                marker.expiresAt shouldBe null
                // The account's lifetime counts from here, and the activation link's with it.
                marker.createdAt shouldBe now

                val verification = createdAuthRecords.filterIsInstance<AuthRecord.EmailVerificationToken>().single()
                verification.ownerId shouldBe UserId(storedUser._id)
                verification.realm shouldBe realm.id
                verification.token shouldBe verificationToken
                verification.expiresAt shouldBe now
                    .plus(realm.tokenConfig.emailVerificationTokenLifetime).toEpochSeconds()

                // The mailed link must carry the token the server will accept, into the route the
                // frontend actually serves.
                activationUrl shouldBe "https://a.b.c/auth/${subject.id}/activate/$verificationToken"
                // ... and the way out for someone who did not sign up: its OWN token, living as long as the
                // ACCOUNT (7 days), not the 24 h activation link.
                val cancel = createdAuthRecords.filterIsInstance<AuthRecord.SignUpCancelToken>().single()
                cancel.ownerId shouldBe UserId(storedUser._id)
                cancel.expiresAt shouldBe now.plus(realm.tokenConfig.pendingAccountLifetime).toEpochSeconds()
                cancelUrl shouldBe "https://a.b.c/auth/${subject.id}/cancel-signup/${cancel.token}"
            }
        }

        "signUp writes the marker FIRST" - {
            "a failing password insert leaves a PENDING (passwordless) account, never an active one" {
                val created = mutableListOf<AuthRecord>()
                val services = lazy {
                    TestServices(
                        onInstantNow = { MpInstant.parse("2026-10-20T10:00:00Z") },
                        onCreateAuthRecord = { create ->
                            val record = create()
                            check(record !is AuthRecord.Password) { "the store fails on the password insert" }
                            created += record
                            Stored(_id = "record-${created.size}", value = record)
                        },
                    )
                }
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                    log = NullLog,
                    services = services,
                )

                shouldThrow<IllegalStateException> {
                    subject.signUp(
                        MinimalTestRealm(
                            onLoadUserByEmail = { null },
                            onCreateUserForSignup = { Stored(_id = "user-id", value = MinimalTestUser()) },
                        ),
                        AuthSignUpRequest.EmailAndPassword(
                            provider = subject.id, email = "new@example.com", password = "A-valid-password-123!",
                        ),
                    )
                }

                created.single().shouldBeInstanceOf<AuthRecord.PendingActivation>()
            }
        }

        "activateAccount" - {

            "should answer success=false for an unknown token, without touching anything" {
                val services = lazy {
                    TestServices(
                        onFindEmailVerificationToken = { _, _ -> null },
                        // `removeAuthRecord` / `removePendingActivations` keep their exploding defaults:
                        // an unknown token must not consume or activate ANYTHING.
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                    log = NullLog,
                    services = services,
                )

                val result = subject.activateAccount(
                    MinimalTestRealm(),
                    AuthActivateAccountRequest(provider = subject.id, token = "no-such-token"),
                )

                result.success shouldBe false
            }

            "should consume the token and drop the marker" {
                val ownerId = UserId("user-id")
                val token = "the-verification-token"

                val removedRecordIds = mutableListOf<String>()
                var markerLookedUpFor: Pair<RealmId, UserId>? = null

                val services = lazy {
                    TestServices(
                        onFindPendingActivation = { realm, owner ->
                            markerLookedUpFor = realm to owner
                            Stored(
                                _id = "pending-activation",
                                value = AuthRecord.PendingActivation(
                                    realm = realm, ownerId = owner, createdAt = Kronos.systemUtc.instantNow(),
                                ),
                            )
                        },
                        onFindEmailVerificationToken = { realm, requested ->
                            requested shouldBe token
                            Stored(
                                _id = "verification-record",
                                value = AuthRecord.EmailVerificationToken(
                                    realm = realm,
                                    ownerId = ownerId,
                                    token = token,
                                    expiresAt = Long.MAX_VALUE,
                                )
                            )
                        },
                        onRemovePendingActivationIfUnclaimed = { removedRecordIds += it; true },
                        onRemoveAuthRecord = { removedRecordIds += it; true },
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                    log = NullLog,
                    services = services,
                )

                val realm = MinimalTestRealm()

                val result = subject.activateAccount(
                    realm,
                    AuthActivateAccountRequest(provider = subject.id, token = token),
                )

                result.success shouldBe true

                // The marker FIRST (that is the activation, and it acted on its own delete count), then the
                // single-use token.
                removedRecordIds shouldBe listOf("pending-activation", "verification-record")

                // ... and the marker is the token's OWNER's, not whoever is calling.
                markerLookedUpFor shouldBe (realm.id to ownerId)
            }
        }

        "resendActivation" - {

            // Expressed against the config rather than magic numbers, so the tests still mean something
            // if the defaults change.
            val now = MpInstant.parse("2026-07-27T12:00:00Z")
            val config = MinimalTestRealm().tokenConfig
            val firstDelay = config.activationResendDelays[0]
            val thirdDelay = config.activationResendDelays[2]

            val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
            val owner = UserId(storedUser._id)

            fun pendingMarker(
                realm: RealmId,
                createdAt: MpInstant,
                resends: Int = 0,
                lastSentAt: MpInstant? = null,
            ) = Stored(
                _id = "pending-activation",
                value = AuthRecord.PendingActivation(
                    realm = realm, ownerId = owner, createdAt = createdAt, resends = resends, lastSentAt = lastSentAt,
                ),
            )

            fun subjectWith(services: Lazy<EmailAndPasswordAuth.Services>) = EmailAndPasswordAuth(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                log = NullLog,
                services = services,
            )

            "should send nothing for an account that no longer exists" {
                // Every other service keeps its exploding default: nothing may be touched.
                val result = subjectWith(lazy { TestServices() }).resendActivation(
                    MinimalTestRealm(
                        getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                        onLoadUserById = { null },
                    ),
                    owner,
                )

                result.sent shouldBe false
                result.nextResendAt shouldBe null
            }

            "should send nothing for an already-activated account" {
                val services = lazy { TestServices(onFindPendingActivation = { _, _ -> null }) }

                val result = subjectWith(services).resendActivation(
                    MinimalTestRealm(
                        getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                        onLoadUserById = { storedUser },
                    ),
                    owner,
                )

                result.sent shouldBe false
            }

            "should send nothing for an EXPIRED pending account" {
                // Expiry is what bounds the resend schedule: past it, the account is retired, not mailed.
                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onFindPendingActivation = { realm, _ ->
                            pendingMarker(realm, createdAt = now.minus(config.pendingAccountLifetime))
                        },
                    )
                }

                val result = subjectWith(services).resendActivation(
                    MinimalTestRealm(
                        getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                        onLoadUserById = { storedUser },
                    ),
                    owner,
                )

                result.sent shouldBe false
                result.nextResendAt shouldBe null
            }

            "should send nothing before the first delay has passed, and say when it will" {
                // `getMessaging` and `savePendingActivation` keep their exploding defaults, so a send or
                // a reserved slot fails the test rather than being asserted after the fact.
                val signedUpAt = now.minus(firstDelay).plus(1.seconds)

                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm, createdAt = signedUpAt) },
                    )
                }

                val result = subjectWith(services).resendActivation(
                    MinimalTestRealm(
                        getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                        onLoadUserById = { storedUser },
                    ),
                    owner,
                )

                result.sent shouldBe false
                result.nextResendAt shouldBe signedUpAt.plus(firstDelay)
            }

            "the delay GROWS: after two resends the next one waits the third delay" {
                // One second short of the THIRD delay. With a flat cooldown (the first delay) this would
                // send, so this is the test that fails if the schedule stops growing.
                val lastSentAt = now.minus(thirdDelay).plus(1.seconds)

                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onFindPendingActivation = { realm, _ ->
                            pendingMarker(
                                realm, createdAt = now.minus(1.days), resends = 2, lastSentAt = lastSentAt,
                            )
                        },
                    )
                }

                val result = subjectWith(services).resendActivation(
                    MinimalTestRealm(
                        getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                        onLoadUserById = { storedUser },
                    ),
                    owner,
                )

                result.sent shouldBe false
                result.nextResendAt shouldBe lastSentAt.plus(thirdDelay)
            }

            "an activation that removed the marker meanwhile wins: nothing is re-inserted, nothing is sent" {
                // A save here would re-insert the marker and make the now-ACTIVE account pending again — and
                // the day-7 sweep would then retire it. `getMessaging` keeps its exploding default.
                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm, createdAt = now.minus(1.days)) },
                        onReplacePendingActivation = { null }, // the marker is gone
                    )
                }

                val result = subjectWith(services).resendActivation(
                    MinimalTestRealm(
                        getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                        onLoadUserById = { storedUser },
                    ),
                    owner,
                )

                result.sent shouldBe false
            }

            "once due: reserves the slot BEFORE mailing, rotates the token, and mails a NEW link" {
                val newToken = "the-new-token"
                val signedUpAt = now.minus(firstDelay)

                val events = mutableListOf<String>()
                var rotation: Triple<RealmId, UserId, String?>? = null
                var saved: AuthRecord.PendingActivation? = null
                var activationUrl: String? = null
                var cancelUrl: String? = null
                val createdAuthRecords = mutableListOf<AuthRecord>()

                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onGenerateToken = { newToken },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm, createdAt = signedUpAt) },
                        onReplacePendingActivation = { marker ->
                            events += "reserve"
                            saved = marker.value()
                            marker
                        },
                        onRemoveEmailVerificationTokens = { realm, o, exceptId ->
                            rotation = Triple(realm, o, exceptId)
                        },
                        // The account's cancel token already exists: every resend mails the SAME one.
                        onFindSignUpCancelTokenOf = { realm, o ->
                            Stored(
                                _id = "cancel-record",
                                value = AuthRecord.SignUpCancelToken(
                                    realm = realm, ownerId = o, expiresAt = Long.MAX_VALUE, token = "the-cancel-token",
                                ),
                            )
                        },
                        onCreateAuthRecord = { create ->
                            val record = create()
                            createdAuthRecords.add(record)
                            @Suppress("UNCHECKED_CAST")
                            Stored(_id = "record-${createdAuthRecords.size}", value = record)
                        },
                    )
                }

                val realm = MinimalTestRealm(
                    getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                    onLoadUserById = { storedUser },
                    getMessaging = {
                        TestMessaging(
                            onSendAccountActivationEmail = { user, url, cancel ->
                                events += "mail"
                                user shouldBe storedUser
                                activationUrl = url
                                cancelUrl = cancel
                                EmailResult.ofMessageId("message-id")
                            }
                        )
                    },
                )

                val result = subjectWith(services).resendActivation(realm, owner)

                result.sent shouldBe true
                // The NEXT delay is the second one, counted from this send.
                result.nextResendAt shouldBe now.plus(config.activationResendDelays[1])

                // The slot is taken before the mail goes out, so a failing or slow mail cannot be
                // retried at full speed.
                events shouldBe listOf("reserve", "mail")
                saved?.resends shouldBe 1
                saved?.lastSentAt shouldBe now

                val issued = createdAuthRecords.filterIsInstance<AuthRecord.EmailVerificationToken>().single()
                issued.token shouldBe newToken
                issued.ownerId shouldBe owner

                // Rotation must EXCLUDE the row just created. Rotating first (or without the exception)
                // lets two interleaved resends delete the token the other has already mailed, so a user
                // receives a link that was dead before it arrived.
                rotation shouldBe Triple(realm.id, owner, "record-1")

                // The resent links must be built exactly like the sign-up ones.
                activationUrl shouldBe "https://a.b.c/auth/${EmailAndPasswordAuth.ID}/activate/$newToken"
                cancelUrl shouldBe "https://a.b.c/auth/${EmailAndPasswordAuth.ID}/cancel-signup/the-cancel-token"
                withClue("no second cancel token is minted") {
                    createdAuthRecords.filterIsInstance<AuthRecord.SignUpCancelToken>() shouldBe emptyList()
                }
            }

            "should answer sent=false when the mail FAILED" {
                // Honest: rotation has already revoked the older link, so claiming success here would
                // tell the user to wait for a mail that is not coming.
                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onGenerateToken = { "token" },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm, createdAt = now.minus(1.days)) },
                        onReplacePendingActivation = { it },
                        onRemoveEmailVerificationTokens = { _, _, _ -> },
                        onFindSignUpCancelTokenOf = { _, _ -> null },
                        onCreateAuthRecord = { create -> Stored(_id = "record", value = create()) },
                    )
                }

                val realm = MinimalTestRealm(
                    getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                    onLoadUserById = { storedUser },
                    getMessaging = {
                        TestMessaging(
                            onSendAccountActivationEmail = { _, _, _ -> EmailResult.ofError(RuntimeException("smtp down")) }
                        )
                    },
                )

                val response = subjectWith(services).resendActivation(realm, owner)

                response.sent shouldBe false
                // A delay here would make the client say "we already sent one recently" — about a failed mail.
                response.nextResendAt shouldBe null
            }

            "the new link never outlives the pending account" {
                // Six and a half days in: a full 24h link would still activate after the account has
                // expired, whether or not the sweep has retired it yet.
                val signedUpAt = now.minus(config.pendingAccountLifetime).plus(12.hours)
                val createdAuthRecords = mutableListOf<AuthRecord>()

                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onGenerateToken = { "token" },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm, createdAt = signedUpAt) },
                        onReplacePendingActivation = { it },
                        onRemoveEmailVerificationTokens = { _, _, _ -> },
                        onFindSignUpCancelTokenOf = { _, _ -> null },
                        onCreateAuthRecord = { create ->
                            val record = create()
                            createdAuthRecords.add(record)
                            Stored(_id = "record", value = record)
                        },
                    )
                }

                val realm = MinimalTestRealm(
                    getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                    onLoadUserById = { storedUser },
                    getMessaging = {
                        TestMessaging(onSendAccountActivationEmail = { _, _, _ -> EmailResult.ofMessageId("id") })
                    },
                )

                subjectWith(services).resendActivation(realm, owner).sent shouldBe true

                createdAuthRecords.filterIsInstance<AuthRecord.EmailVerificationToken>().single()
                    .expiresAt shouldBe signedUpAt.plus(config.pendingAccountLifetime).toEpochSeconds()
            }
        }

        "cancelSignUp" - {

            fun subjectWith(services: Lazy<EmailAndPasswordAuth.Services>) = EmailAndPasswordAuth(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                log = NullLog,
                services = services,
            )

            "should answer success=false for an unknown token, without touching anything" {
                val services = lazy {
                    TestServices(
                        onFindSignUpCancelToken = { _, _ -> null },
                        onFindEmailVerificationToken = { _, _ -> null },
                    )
                }

                subjectWith(services).cancelSignUp(
                    MinimalTestRealm(),
                    AuthCancelSignUpRequest(provider = EmailAndPasswordAuth.ID, token = "nope"),
                ).success shouldBe false
            }

            "the token is NOT consumed up front: the retire decides, and a refused retire leaves it usable" {
                // `onRemoveAuthRecord` keeps its exploding default; the realm's (empty) store has no marker, so
                // its retire refuses.
                val services = lazy {
                    TestServices(
                        onFindSignUpCancelToken = { realm, token ->
                            Stored(
                                _id = "cancel-record",
                                value = AuthRecord.SignUpCancelToken(
                                    realm = realm, ownerId = UserId("user-id"), expiresAt = Long.MAX_VALUE, token = token,
                                ),
                            )
                        },
                    )
                }

                subjectWith(services).cancelSignUp(
                    MinimalTestRealm(),
                    AuthCancelSignUpRequest(provider = EmailAndPasswordAuth.ID, token = "t"),
                ).success shouldBe false
            }
        }

        "recoverAccountSetPasswordWithToken" - {

            "should ALSO activate the account — the reset link proves the same mailbox" {
                // The way back for a pending owner who does not know the sign-up password (e.g. one an
                // attacker chose when pre-registering the address): the new password supersedes it.

                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
                val ownerId = UserId(storedUser._id)

                var activatedOwner: Pair<RealmId, UserId>? = null
                val removedIds = mutableListOf<String>()

                val services = lazy {
                    TestServices(
                        onHashPassword = { "hashed-password" },
                        onFindPasswordRecoveryToken = { realm, token ->
                            Stored(
                                _id = "recovery-record",
                                value = AuthRecord.PasswordRecoveryToken(
                                    realm = realm,
                                    ownerId = ownerId,
                                    token = token,
                                    expiresAt = Long.MAX_VALUE,
                                )
                            )
                        },
                        onCreateAuthRecord = { create ->
                            @Suppress("UNCHECKED_CAST")
                            Stored(_id = "new-password-record", value = create())
                        },
                        onFindPendingActivation = { realm, owner ->
                            activatedOwner = realm to owner
                            Stored(
                                _id = "pending-activation",
                                value = AuthRecord.PendingActivation(
                                    realm = realm, ownerId = owner, createdAt = Kronos.systemUtc.instantNow(),
                                ),
                            )
                        },
                        onRemovePendingActivationIfUnclaimed = { removedIds += it; true },
                        onRemoveAuthRecord = { removedIds += it; true },
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                    log = NullLog,
                    services = services,
                )

                val realm = MinimalTestRealm(
                    onLoadUserById = { storedUser },
                    getMessaging = {
                        TestMessaging(
                            onSendPasswordChangedEmail = { EmailResult.ofMessageId("message-id") }
                        )
                    },
                )

                val result = subject.recoverAccountSetPasswordWithToken(
                    realm,
                    AuthRecoverAccountRequest.SetPasswordWithToken(
                        provider = subject.id,
                        token = "the-recovery-token",
                        password = "A-valid-password-123!",
                    ),
                )

                result.success shouldBe true

                activatedOwner shouldBe (realm.id to ownerId)
                removedIds shouldBe listOf("recovery-record", "pending-activation")
            }
        }

        "recoverAccountSetPasswordWithToken on a pending account that is not plainly live" - {

            fun recoveryToken(realm: RealmId) = Stored(
                _id = "recovery-record",
                value = AuthRecord.PasswordRecoveryToken(
                    realm = realm, ownerId = UserId("user-id"), token = "t", expiresAt = Long.MAX_VALUE,
                ),
            )

            fun subjectWith(services: Lazy<EmailAndPasswordAuth.Services>) = EmailAndPasswordAuth(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                log = NullLog,
                services = services,
            )

            val request = AuthRecoverAccountRequest.SetPasswordWithToken(provider = "email-password", token = "t", password = "Pw-123456!")

            "a token another completion consumed first does nothing" {
                // `createAuthRecord` keeps its exploding default.
                val services = lazy {
                    TestServices(
                        onFindPasswordRecoveryToken = { realm, _ -> recoveryToken(realm) },
                        onRemoveAuthRecord = { false },
                    )
                }

                subjectWith(services).recoverAccountSetPasswordWithToken(MinimalTestRealm(), request).success shouldBe false
            }

            "an account a retire has CLAIMED is not this reset's to finish" {
                val services = lazy {
                    TestServices(
                        onFindPasswordRecoveryToken = { realm, _ -> recoveryToken(realm) },
                        onRemoveAuthRecord = { true },
                        onFindPendingActivation = { realm, owner ->
                            Stored(
                                _id = "pending-activation",
                                value = AuthRecord.PendingActivation(
                                    realm = realm, ownerId = owner, createdAt = Kronos.systemUtc.instantNow(),
                                    claimedBy = AuthRecord.PendingActivation.CLAIMED_BY_RETIRE,
                                ),
                            )
                        },
                    )
                }

                subjectWith(services).recoverAccountSetPasswordWithToken(MinimalTestRealm(), request).success shouldBe false
            }

            "an EXPIRED pending account is not revived by a reset link issued before it expired" {
                // `createAuthRecord` keeps its exploding default: no password may be written.
                val services = lazy {
                    TestServices(
                        onFindPasswordRecoveryToken = { realm, _ -> recoveryToken(realm) },
                        onRemoveAuthRecord = { true },
                        onFindPendingActivation = { realm, owner ->
                            Stored(
                                _id = "pending-activation",
                                value = AuthRecord.PendingActivation(
                                    realm = realm, ownerId = owner,
                                    createdAt = Kronos.systemUtc.instantNow().minus(RealmTokenConfig().pendingAccountLifetime),
                                ),
                            )
                        },
                    )
                }

                subjectWith(services).recoverAccountSetPasswordWithToken(MinimalTestRealm(), request).success shouldBe false
            }

            "a retire that claimed the marker meanwhile takes back exactly the password this reset wrote" {
                val removed = mutableListOf<String>()

                val services = lazy {
                    TestServices(
                        onHashPassword = { "hash" },
                        onFindPasswordRecoveryToken = { realm, _ -> recoveryToken(realm) },
                        onCreateAuthRecord = { create -> Stored(_id = "new-password", value = create()) },
                        onFindPendingActivation = { realm, owner ->
                            Stored(
                                _id = "pending-activation",
                                value = AuthRecord.PendingActivation(
                                    realm = realm, ownerId = owner, createdAt = Kronos.systemUtc.instantNow(),
                                ),
                            )
                        },
                        onRemoveAuthRecord = { id -> removed += id; true },
                        // A retire claimed the marker meanwhile.
                        onRemovePendingActivationIfUnclaimed = { false },
                    )
                }

                subjectWith(services).recoverAccountSetPasswordWithToken(MinimalTestRealm(), request).success shouldBe false
                // The token, then EXACTLY the password this reset wrote — not any other.
                removed shouldBe listOf("recovery-record", "new-password")
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
                    userId = UserId("user-id"),
                    currentPassword = "old-password",
                    newPassword = "weak",
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
                    userId = UserId("user-id"),
                    currentPassword = "old-password",
                    newPassword = "A-valid-password-123!",
                )

                val realm = MinimalTestRealm(
                    onLoadUserById = {
                        it shouldBe UserId("user-id")
                        null // User not found
                    }
                )

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.setPassword(realm, request)
                }
                error.message shouldBe "User 'user-id' not found"
            }

            "should throw invalidCredentials when current password is wrong" {
                // Setup
                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())

                val services = lazy {
                    TestServices(
                        onFindLatestPasswordRecord = { realm, owner ->
                            Stored(
                                _id = "password-record",
                                value = AuthRecord.Password(
                                    realm = realm,
                                    ownerId = owner,
                                    token = "hashed-current-password",
                                )
                            )
                        },
                        onCheckPassword = { _, _ ->
                            false // wrong password
                        },
                    )
                }
                val subject = EmailAndPasswordAuth(
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSetPasswordRequest(
                    provider = subject.id,
                    userId = UserId(storedUser._id),
                    currentPassword = "wrong-password",
                    newPassword = "Strong-password-123!",
                )
                val realm = MinimalTestRealm(
                    onLoadUserById = {
                        it shouldBe UserId(storedUser._id)
                        storedUser
                    },
                )

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.setPassword(realm, request)
                }
                error.message shouldBe "Invalid credentials"
            }

            "should set the password successfully when current password is correct" {
                // Setup
                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
                var passwordEmailSent = false
                val currentPassword = "Current-password-123!"
                val newPassword = "Strong-password-123!"
                val hashedPassword = "hashed-new-password"

                val services = lazy {
                    TestServices(
                        onFindLatestPasswordRecord = { realm, owner ->
                            Stored(
                                _id = "password-record",
                                value = AuthRecord.Password(
                                    realm = realm,
                                    ownerId = owner,
                                    token = "hashed-current-password",
                                )
                            )
                        },
                        onCheckPassword = { plaintext, _ ->
                            plaintext == currentPassword
                        },
                        onHashPassword = {
                            it shouldBe newPassword
                            hashedPassword
                        },
                        onCreateAuthRecord = { create ->
                            val record = create() as AuthRecord.Password
                            record.realm shouldBe RealmId("test-realm")
                            record.ownerId shouldBe UserId(storedUser._id)
                            record.token shouldBe hashedPassword

                            Stored(_id = "new-password-record", value = record)
                        },
                    )
                }
                val subject = EmailAndPasswordAuth(
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSetPasswordRequest(
                    provider = subject.id,
                    userId = UserId(storedUser._id),
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                )
                val realm = MinimalTestRealm(
                    onLoadUserById = {
                        it shouldBe UserId(storedUser._id)
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
