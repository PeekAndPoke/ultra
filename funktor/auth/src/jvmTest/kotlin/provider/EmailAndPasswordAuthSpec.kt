package io.peekandpoke.funktor.auth.provider

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.MinimalTestDeps
import io.peekandpoke.funktor.auth.MinimalTestRealm
import io.peekandpoke.funktor.auth.MinimalTestUser
import io.peekandpoke.funktor.auth.TestMessaging
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Stored
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
        val onFindLatestEmailVerificationToken: suspend (RealmId, UserId) -> Stored<AuthRecord.EmailVerificationToken>? =
            { _, _ -> error("findLatestEmailVerificationToken not implemented") },
        val onRemoveEmailVerificationTokens: suspend (RealmId, UserId, String?) -> Unit =
            { _, _, _ -> error("removeEmailVerificationTokens not implemented") },
        val onFindActivationResendToken: suspend (RealmId, String) -> Stored<AuthRecord.ActivationResendToken>? =
            { _, _ -> error("findActivationResendToken not implemented") },
        val onFindPendingActivation: suspend (RealmId, UserId) -> Stored<AuthRecord.PendingActivation>? =
            { _, _ -> error("findPendingActivation not implemented") },
        val onRemovePendingActivations: suspend (RealmId, UserId) -> Unit =
            { _, _ -> error("removePendingActivations not implemented") },
        val onRemoveAuthRecord: suspend (String) -> Unit = { error("removeAuthRecord not implemented") },
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

        override suspend fun findLatestEmailVerificationToken(
            realm: RealmId,
            owner: UserId,
        ): Stored<AuthRecord.EmailVerificationToken>? = onFindLatestEmailVerificationToken(realm, owner)

        override suspend fun removeEmailVerificationTokens(realm: RealmId, owner: UserId, exceptId: String?) =
            onRemoveEmailVerificationTokens(realm, owner, exceptId)

        override suspend fun findActivationResendToken(
            realm: RealmId,
            token: String,
        ): Stored<AuthRecord.ActivationResendToken>? = onFindActivationResendToken(realm, token)

        override suspend fun findPendingActivation(
            realm: RealmId,
            owner: UserId,
        ): Stored<AuthRecord.PendingActivation>? = onFindPendingActivation(realm, owner)

        override suspend fun removePendingActivations(realm: RealmId, owner: UserId) =
            onRemovePendingActivations(realm, owner)

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
                            onSendAccountActivationEmail = { user, url ->
                                user shouldBe storedUser
                                activationUrl = url
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

                val verification = createdAuthRecords.filterIsInstance<AuthRecord.EmailVerificationToken>().single()
                verification.ownerId shouldBe UserId(storedUser._id)
                verification.realm shouldBe realm.id
                verification.token shouldBe verificationToken
                verification.expiresAt shouldBe now
                    .plus(realm.tokenConfig.emailVerificationTokenLifetime).toEpochSeconds()

                // The mailed link must carry the token the server will accept, into the route the
                // frontend actually serves.
                activationUrl shouldBe "https://a.b.c/auth/${subject.id}/activate/$verificationToken"
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

                var removedRecordId: String? = null
                var activatedOwner: Pair<RealmId, UserId>? = null

                val services = lazy {
                    TestServices(
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
                        onRemovePendingActivations = { realm, owner -> activatedOwner = realm to owner },
                        onRemoveAuthRecord = { removedRecordId = it },
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

                // Single-use: the token record is gone.
                removedRecordId shouldBe "verification-record"

                // ... and the marker is dropped for the token's OWNER, not for whoever is calling.
                activatedOwner shouldBe (realm.id to ownerId)
            }
        }

        "resendActivation" - {

            // The cooldown boundary, expressed against the config rather than a magic number so it
            // still means something if the default changes.
            val now = MpInstant.parse("2026-07-27T12:00:00Z")
            val cooldown = MinimalTestRealm().tokenConfig.activationResendCooldown

            val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
            val owner = UserId(storedUser._id)
            val resendToken = "the-resend-token"

            fun resendTokenRecord(realm: RealmId) = Stored(
                _id = "resend-token-record",
                value = AuthRecord.ActivationResendToken(
                    realm = realm,
                    ownerId = owner,
                    token = resendToken,
                    expiresAt = Long.MAX_VALUE,
                ),
            )

            fun pendingMarker(realm: RealmId) = Stored(
                _id = "pending-activation",
                value = AuthRecord.PendingActivation(realm = realm, ownerId = owner),
            )

            fun verificationToken(realm: RealmId, createdAt: MpInstant) = Stored(
                _id = "verification-record",
                value = AuthRecord.EmailVerificationToken(
                    realm = realm,
                    ownerId = owner,
                    token = "previous-token",
                    expiresAt = Long.MAX_VALUE,
                    createdAt = createdAt,
                ),
            )

            fun subjectWith(services: Lazy<EmailAndPasswordAuth.Services>) = EmailAndPasswordAuth(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                log = NullLog,
                services = services,
            )

            "should send nothing for an unknown resend token" {
                // THE authorization. Every service beyond the token lookup keeps its exploding default,
                // so an unauthorized call cannot touch storage OR mail — it must not even resolve a
                // user, which is what made the email-keyed version an anonymous mail primitive.
                val services = lazy { TestServices(onFindActivationResendToken = { _, _ -> null }) }

                val result = subjectWith(services).resendActivation(
                    MinimalTestRealm(),
                    AuthResendActivationRequest(provider = EmailAndPasswordAuth.ID, token = "nope"),
                )

                result.sent shouldBe false
            }

            "should consume the token even when nothing is sent, so one sign-in buys one resend" {
                var removedRecordId: String? = null

                val services = lazy {
                    TestServices(
                        onFindActivationResendToken = { realm, _ -> resendTokenRecord(realm) },
                        onRemoveAuthRecord = { removedRecordId = it },
                        // No marker = already activated, so no mail. The token is spent regardless.
                        onFindPendingActivation = { _, _ -> null },
                    )
                }

                val realm = MinimalTestRealm(onLoadUserById = { storedUser })

                val result = subjectWith(services).resendActivation(
                    realm,
                    AuthResendActivationRequest(provider = EmailAndPasswordAuth.ID, token = resendToken),
                )

                result.sent shouldBe false
                removedRecordId shouldBe "resend-token-record"
            }

            "should send nothing while inside the cooldown window" {
                // The second throttle, behind the token: re-authenticating in a loop still cannot mail
                // faster than this. `getMessaging` keeps its exploding default, so a send fails the
                // test rather than being asserted after the fact.
                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onFindActivationResendToken = { realm, _ -> resendTokenRecord(realm) },
                        onRemoveAuthRecord = { },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm) },
                        onFindLatestEmailVerificationToken = { realm, _ ->
                            // Issued one second inside the window.
                            verificationToken(realm, now.minus(cooldown).plus(1.seconds))
                        },
                    )
                }

                val realm = MinimalTestRealm(onLoadUserById = { storedUser })

                val result = subjectWith(services).resendActivation(
                    realm,
                    AuthResendActivationRequest(provider = EmailAndPasswordAuth.ID, token = resendToken),
                )

                result.sent shouldBe false
            }

            "should rotate the token and mail a NEW link once the cooldown has passed" {
                val newToken = "the-new-token"

                var rotation: Triple<RealmId, UserId, String?>? = null
                var activationUrl: String? = null
                val createdAuthRecords = mutableListOf<AuthRecord>()

                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onGenerateToken = { newToken },
                        onFindActivationResendToken = { realm, _ -> resendTokenRecord(realm) },
                        onRemoveAuthRecord = { },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm) },
                        onFindLatestEmailVerificationToken = { realm, _ ->
                            // Exactly ON the boundary — the cooldown has elapsed, so this must send.
                            verificationToken(realm, now.minus(cooldown))
                        },
                        onRemoveEmailVerificationTokens = { realm, o, exceptId ->
                            rotation = Triple(realm, o, exceptId)
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
                    onLoadUserById = { storedUser },
                    getMessaging = {
                        TestMessaging(
                            onSendAccountActivationEmail = { user, url ->
                                user shouldBe storedUser
                                activationUrl = url
                                EmailResult.ofMessageId("message-id")
                            }
                        )
                    },
                )

                val result = subjectWith(services).resendActivation(
                    realm,
                    AuthResendActivationRequest(provider = EmailAndPasswordAuth.ID, token = resendToken),
                )

                result.sent shouldBe true

                val issued = createdAuthRecords.filterIsInstance<AuthRecord.EmailVerificationToken>().single()
                issued.token shouldBe newToken
                issued.ownerId shouldBe owner

                // Rotation must EXCLUDE the row just created. Rotating first (or without the exception)
                // lets two interleaved resends delete the token the other has already mailed, so a user
                // receives a link that was dead before it arrived.
                rotation shouldBe Triple(realm.id, owner, "record-1")

                // The resent link must be built exactly like the sign-up one. A resend that produced a
                // different URL shape would be a dead link that no server-side test could see.
                activationUrl shouldBe "https://a.b.c/auth/${EmailAndPasswordAuth.ID}/activate/$newToken"
            }
        }

        "recoverAccountSetPasswordWithToken" - {

            "should ALSO activate the account — the reset link proves the same mailbox" {
                // This is the only way back for an account whose activation link expired, because
                // there is no resend endpoint yet. If this ever goes red, the account is not merely
                // un-activated: it is permanently unreachable.

                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
                val ownerId = UserId(storedUser._id)

                var activatedOwner: Pair<RealmId, UserId>? = null

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
                        onRemovePendingActivations = { realm, owner -> activatedOwner = realm to owner },
                        onRemoveAuthRecord = { },
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
