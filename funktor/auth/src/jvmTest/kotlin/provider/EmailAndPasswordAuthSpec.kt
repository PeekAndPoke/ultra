package io.peekandpoke.funktor.auth.provider

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.funktor.auth.AccountRetirement
import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.MinimalTestDeps
import io.peekandpoke.funktor.auth.MinimalTestRealm
import io.peekandpoke.funktor.auth.MinimalTestUser
import io.peekandpoke.funktor.auth.NullStorageTestDeps
import io.peekandpoke.funktor.auth.RealmTokenConfig
import io.peekandpoke.funktor.auth.TestMessaging
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.domain.TokenHash
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthDisplayName
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class EmailAndPasswordAuthSpec : FreeSpec() {

    private open class TestServices(
        val onHashPassword: (String) -> String = { error("hashPassword not implemented") },
        val onCheckPassword: (String, String) -> Boolean = { _, _ -> error("checkPassword not implemented") },
        val onPasswordNeedsRehash: (String) -> Boolean = { false },
        val onReplacePasswordRecord: suspend (Stored<AuthRecord.Password>) -> Boolean =
            { error("replacePasswordRecord not implemented") },
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
        val onCapEmailVerificationTokens: suspend (RealmId, UserId, String, MpInstant) -> Unit =
            { _, _, _, _ -> error("capEmailVerificationTokens not implemented") },
        val onRemovePasswordRecords: suspend (RealmId, UserId, String?) -> Unit =
            { _, _, _ -> error("removePasswordRecords not implemented") },
        val onFindPendingActivation: suspend (RealmId, UserId) -> Stored<AuthRecord.PendingActivation>? =
            { _, _ -> error("findPendingActivation not implemented") },
        val onReplacePendingActivation: suspend (
            read: Stored<AuthRecord.PendingActivation>, next: Stored<AuthRecord.PendingActivation>,
        ) -> Stored<AuthRecord.PendingActivation>? =
            { _, _ -> error("replacePendingActivation not implemented") },
        val onFindSignUpCancelToken: suspend (RealmId, String) -> Stored<AuthRecord.SignUpCancelToken>? =
            { _, _ -> error("findSignUpCancelToken not implemented") },
        val onRemoveSignUpCancelTokens: suspend (RealmId, UserId) -> Unit =
            { _, _ -> error("removeSignUpCancelTokens not implemented") },
        val onTrimSignUpCancelTokens: suspend (RealmId, UserId, String, Int) -> Unit =
            { _, _, _, _ -> error("trimSignUpCancelTokens not implemented") },
        val onRemovePendingActivationIfUnclaimed: suspend (String) -> Boolean =
            { error("removePendingActivationIfUnclaimed not implemented") },
        val onRemoveAuthRecord: suspend (String) -> Boolean = { error("removeAuthRecord not implemented") },
    ) : EmailAndPasswordAuth.Services {
        override fun hashPassword(password: String): String = onHashPassword(password)
        override fun checkPassword(plaintext: String, hash: String): Boolean = onCheckPassword(plaintext, hash)
        override fun passwordNeedsRehash(hash: String): Boolean = onPasswordNeedsRehash(hash)
        override suspend fun replacePasswordRecord(record: Stored<AuthRecord.Password>): Boolean =
            onReplacePasswordRecord(record)
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

        override suspend fun capEmailVerificationTokens(realm: RealmId, owner: UserId, exceptId: String, notAfter: MpInstant) =
            onCapEmailVerificationTokens(realm, owner, exceptId, notAfter)

        override suspend fun removePasswordRecords(realm: RealmId, owner: UserId, exceptId: String?) =
            onRemovePasswordRecords(realm, owner, exceptId)

        override suspend fun findPendingActivation(
            realm: RealmId,
            owner: UserId,
        ): Stored<AuthRecord.PendingActivation>? = onFindPendingActivation(realm, owner)

        override suspend fun replacePendingActivation(
            read: Stored<AuthRecord.PendingActivation>,
            next: Stored<AuthRecord.PendingActivation>,
        ): Stored<AuthRecord.PendingActivation>? = onReplacePendingActivation(read, next)

        override suspend fun findSignUpCancelToken(realm: RealmId, token: String) = onFindSignUpCancelToken(realm, token)

        override suspend fun removeSignUpCancelTokens(realm: RealmId, owner: UserId) =
            onRemoveSignUpCancelTokens(realm, owner)

        override suspend fun trimSignUpCancelTokens(realm: RealmId, owner: UserId, exceptId: String, keep: Int) =
            onTrimSignUpCancelTokens(realm, owner, exceptId, keep)

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

            "a password over 256 characters is refused before the user is even loaded, so the address cannot matter" {
                // ULTRA-00296: the realm's loadByEmail and every service throw if reached.
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    log = NullLog,
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    services = lazy { TestServices() },
                )

                listOf(257, 100_000).forEach { length ->
                    shouldThrow<AuthError> {
                        subject.signIn(
                            MinimalTestRealm(),
                            AuthSignInRequest.EmailAndPassword(provider = subject.id, email = "user@example.com", password = "x".repeat(length)),
                        )
                    }.message shouldBe "Invalid credentials"
                }
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

            "rehash (ULTRA-00066): a proven password with an OUTDATED hash is re-hashed in place; a current one is not" {
                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
                val record = Stored(
                    _id = "password-id",
                    value = AuthRecord.Password(realm = RealmId("test-realm"), ownerId = UserId(storedUser._id), token = "old-hash"),
                )

                fun subjectWith(outdated: Boolean, replace: suspend (Stored<AuthRecord.Password>) -> Boolean) =
                    EmailAndPasswordAuth(
                        capabilities = setOf(AuthProviderModel.Capability.SignIn),
                        log = NullLog,
                        frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                        services = lazy {
                            TestServices(
                                onCheckPassword = { plaintext, _ -> plaintext == "correct-password" },
                                onPasswordNeedsRehash = { it == "old-hash" && outdated },
                                onHashPassword = { "new-hash-of-$it" },
                                onReplacePasswordRecord = replace,
                                onFindLatestPasswordRecord = { _, _ -> record },
                                onFindPendingActivation = { _, _ -> null },
                            )
                        },
                    )

                fun request(password: String) =
                    AuthSignInRequest.EmailAndPassword(provider = EmailAndPasswordAuth.ID, email = "user@example.com", password = password)

                val realm = MinimalTestRealm(onLoadUserByEmail = { storedUser })

                withClue("outdated: the SAME record (in place), the new hash of the proven password") {
                    val written = mutableListOf<Stored<AuthRecord.Password>>()
                    subjectWith(outdated = true) { written += it; true }.signIn(realm, request("correct-password")) shouldBe storedUser

                    written.single()._id shouldBe "password-id"
                    written.single().resolve().token shouldBe "new-hash-of-correct-password"
                }

                withClue("current: nothing written") {
                    val written = mutableListOf<Stored<AuthRecord.Password>>()
                    subjectWith(outdated = false) { written += it; true }.signIn(realm, request("correct-password")) shouldBe
                        storedUser

                    written shouldBe emptyList()
                }

                withClue("a wrong password re-hashes nothing, outdated or not") {
                    val written = mutableListOf<Stored<AuthRecord.Password>>()
                    shouldThrow<AuthError> {
                        subjectWith(outdated = true) { written += it; true }.signIn(realm, request("wrong-password"))
                    }

                    written shouldBe emptyList()
                }

                withClue("a new hash the hasher cannot verify is never written (a compound nested in a compound)") {
                    val written = mutableListOf<Stored<AuthRecord.Password>>()
                    val subject = EmailAndPasswordAuth(
                        capabilities = setOf(AuthProviderModel.Capability.SignIn),
                        log = NullLog,
                        frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                        services = lazy {
                            TestServices(
                                // The stored hash checks; what the primary makes now does not.
                                onCheckPassword = { plaintext, hash -> plaintext == "correct-password" && hash == "old-hash" },
                                onPasswordNeedsRehash = { true },
                                onHashPassword = { "unverifiable-$it" },
                                onReplacePasswordRecord = { written += it; true },
                                onFindLatestPasswordRecord = { _, _ -> record },
                                onFindPendingActivation = { _, _ -> null },
                            )
                        },
                    )

                    subject.signIn(realm, request("correct-password")) shouldBe storedUser
                    written shouldBe emptyList()
                }

                withClue("a failing rewrite never fails the sign-in, also when the store's failure is no Exception (Karango's)") {
                    subjectWith(outdated = true) { throw object : Throwable("the store is down") {} }
                        .signIn(realm, request("correct-password")) shouldBe storedUser
                }
            }

            "a pending account signed up BEFORE email first refuses its sign-up password, exactly like a wrong one" {
                // The credentials are CORRECT here. Anything less would pass for the wrong reason —
                // the point is that a valid password is not enough until the address is proven. Since email
                // first, a pending account has no password at all; one signed up before still has it.

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

                error.message shouldBe "Invalid credentials"
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

            "a CLAIMED account refuses sign-in, even with the right password" {
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

                error.message shouldBe "Invalid credentials"
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
                    email = " "
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
                    email = "this-is-not-an-email"
                )
                val realm = MinimalTestRealm()

                // Execute & Verify
                val error = shouldThrow<AuthError> {
                    subject.signUp(realm, request)
                }
                error.message shouldBe "Invalid request"
            }

            "a sign-up whose user cannot be created (a concurrent sign-up won the index) is refused, also for a Throwable" {
                // A Karango user repository fails with Throwables, not Exceptions; that was a 500.
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = lazy { TestServices() },
                )
                listOf(
                    object : Throwable("unique constraint violated") {} to "User already exists",
                    // A realm without sign-up refuses with its own AuthError: its message survives.
                    AuthError.notSupported() to "Not supported",
                ).forEach { (failure, answer) ->
                    val realm = MinimalTestRealm(onLoadUserByEmail = { null }, onCreateUserForSignup = { throw failure })

                    withClue(failure.message) {
                        shouldThrow<AuthError> {
                            subject.signUp(realm, AuthSignUpRequest.EmailAndPassword(provider = subject.id, email = "new@example.com"))
                        }.message shouldBe answer
                    }
                }
            }

            "should throw when an ACTIVE user already exists" {
                // Setup
                val services = lazy { TestServices(onFindPendingActivation = { _, _ -> null }) }
                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignUp),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = services,
                )
                val request = AuthSignUpRequest.EmailAndPassword(
                    provider = subject.id,
                    email = "test@example.com"
                )
                // An ACTIVE account: no marker. `getMessaging` keeps its exploding default: nothing is mailed.
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
                val email = "test@example.com"
                val verificationToken = "the-verification-token"
                val generated = ArrayDeque(listOf(verificationToken, "the-cancel-token"))
                val now = MpInstant.parse("2026-07-27T10:00:00Z")

                val createdAuthRecords = mutableListOf<AuthRecord>()
                var activationUrl: String? = null
                var cancelUrl: String? = null

                val services = lazy {
                    // `onHashPassword` keeps its exploding default: sign-up never touches a password.
                    TestServices(
                        onCreateAuthRecord = { create ->
                            val record = create()
                            createdAuthRecords.add(record)
                            @Suppress("UNCHECKED_CAST")
                            Stored(_id = "record-${createdAuthRecords.size}", value = record)
                        },
                        onGenerateToken = { generated.removeFirst() },
                        onInstantNow = { now },
                        // A brand-new user has no earlier tokens; the shared issue-and-send path calls
                        // this anyway, so the sign-up and re-sent links cannot drift apart.
                        onCapEmailVerificationTokens = { _, _, _, _ -> },
                        onTrimSignUpCancelTokens = { _, _, _, _ -> },
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
                    email = email
                )

                val realm = MinimalTestRealm(
                    passwordPolicy = PasswordPolicy.default,
                    onLoadUserByEmail = { null },
                    onCreateUserForSignup = { params ->
                        params.email shouldBe EmailAddress.of(email)
                        // Sign-up takes no name: the default from the address, until the activation page sets one.
                        params.displayName shouldBe "test"
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

                withClue("email first: a pending account holds NO credential") {
                    createdAuthRecords.filterIsInstance<AuthRecord.Password>() shouldBe emptyList()
                }

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
                withClue("stored as its hash, never in clear text") {
                    verification.tokenHash shouldBe TokenHash.of(verificationToken)
                    verification.token shouldBe null
                }
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
                cancel.tokenHash shouldBe TokenHash.of("the-cancel-token")
                cancelUrl shouldBe "https://a.b.c/auth/${subject.id}/cancel-signup/the-cancel-token"
            }
        }

        "signUp writes the marker FIRST" - {
            "a failing link insert leaves a PENDING account, never an active one" {
                val created = mutableListOf<AuthRecord>()
                val services = lazy {
                    TestServices(
                        onInstantNow = { MpInstant.parse("2026-10-20T10:00:00Z") },
                        onCreateAuthRecord = { create ->
                            val record = create()
                            check(record !is AuthRecord.EmailVerificationToken) { "the store fails on the link insert" }
                            created += record
                            Stored(_id = "record-${created.size}", value = record)
                        },
                        onGenerateToken = { "token" },
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
                        AuthSignUpRequest.EmailAndPassword(provider = subject.id, email = "new@example.com"),
                    )
                }

                created.single().shouldBeInstanceOf<AuthRecord.PendingActivation>()
            }
        }

        "activateAccount" - {

            val ownerId = UserId("user-id")
            val storedUser = Stored(_id = ownerId.value, value = MinimalTestUser())

            fun subjectWith(services: Lazy<EmailAndPasswordAuth.Services>) = EmailAndPasswordAuth(
                capabilities = setOf(AuthProviderModel.Capability.SignUp),
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                log = NullLog,
                services = services,
            )

            fun linkRecord(realm: RealmId, token: String) = Stored(
                _id = "verification-record",
                value = AuthRecord.EmailVerificationToken(
                    realm = realm, ownerId = ownerId, tokenHash = TokenHash.of(token), expiresAt = Long.MAX_VALUE,
                ),
            )

            fun pendingMarker(realm: RealmId) = Stored(
                _id = "pending-activation",
                value = AuthRecord.PendingActivation(
                    realm = realm, ownerId = ownerId, createdAt = Kronos.systemUtc.instantNow(),
                ),
            )

            "should answer null for an unknown token, without touching anything" {
                val services = lazy {
                    TestServices(
                        onFindEmailVerificationToken = { _, _ -> null },
                        // Everything else keeps its exploding default: an unknown token must not consume,
                        // write or activate ANYTHING.
                    )
                }

                subjectWith(services).activateAccount(
                    MinimalTestRealm(),
                    AuthActivateAccountRequest(
                        provider = EmailAndPasswordAuth.ID, token = "no-such-token", password = "New-1234!",
                        displayName = "Test User",
                    ),
                ) shouldBe null
            }

            "a WEAK password or a bad NAME is refused by the realm before the token is even looked up — the link stays usable" {
                // Every service keeps its exploding default: the provider is never asked.
                val subject = subjectWith(lazy { TestServices() })
                val realm = MinimalTestRealm(passwordPolicy = PasswordPolicy.default, getProviders = { listOf(subject) })

                fun request(password: String, displayName: String) = AuthActivateAccountRequest(
                    provider = subject.id, token = "t", password = password, displayName = displayName,
                )

                realm.activate(request(password = "weak", displayName = "Ada")) shouldBe
                    AuthActivateAccountResponse(success = false, failure = AuthActivateAccountResponse.Failure.WeakPassword)

                for (name in listOf("", "   ", "x".repeat(AuthDisplayName.MAX_LENGTH + 1), "Ada${Char(0x0A)}Lovelace", "Ada${Char(0x202E)}")) {
                    withClue("name '$name'") {
                        realm.activate(request(password = "New-1234!", displayName = name)) shouldBe AuthActivateAccountResponse(
                            success = false, failure = AuthActivateAccountResponse.Failure.InvalidDisplayName,
                        )
                    }
                }
            }

            "the PROVIDER refuses a weak password itself, before the token, for a realm that calls it directly" {
                // Every service keeps its exploding default: the token is never looked up.
                subjectWith(lazy { TestServices() }).activateAccount(
                    MinimalTestRealm(passwordPolicy = PasswordPolicy.default),
                    AuthActivateAccountRequest(
                        provider = EmailAndPasswordAuth.ID, token = "t", password = "weak", displayName = "Ada",
                    ),
                ) shouldBe null
            }

            "consumes the link FIRST, then sets the chosen password, then drops the marker" {
                val token = "the-verification-token"
                val events = mutableListOf<String>()
                var markerLookedUpFor: Pair<RealmId, UserId>? = null
                val created = mutableListOf<AuthRecord>()

                val services = lazy {
                    TestServices(
                        onFindEmailVerificationToken = { realm, requested ->
                            requested shouldBe token
                            linkRecord(realm, token)
                        },
                        onFindPendingActivation = { realm, owner ->
                            markerLookedUpFor = realm to owner
                            pendingMarker(realm)
                        },
                        onRemoveAuthRecord = { events += "remove:$it"; true },
                        onRemovePasswordRecords = { _, owner, exceptId -> events += "remove-passwords:${owner.value}-except:$exceptId" },
                        onHashPassword = { "hash($it)" },
                        onCreateAuthRecord = { create ->
                            val record = create()
                            created += record
                            events += "create:${record::class.simpleName}"
                            Stored(_id = "new-password", value = record)
                        },
                        onRemovePendingActivationIfUnclaimed = { events += "activate:$it"; true },
                        onRemoveEmailVerificationTokens = { _, _, _ -> events += "remove-links" },
                        onRemoveSignUpCancelTokens = { _, owner -> events += "remove-cancel-links:${owner.value}" },
                    )
                }

                val realm = MinimalTestRealm(onLoadUserById = { storedUser })

                subjectWith(services).activateAccount(
                    realm,
                    AuthActivateAccountRequest(
                        provider = EmailAndPasswordAuth.ID, token = token, password = "New-1234!",
                        displayName = "Test User",
                    ),
                ) shouldBe storedUser

                // The link by its delete count FIRST (single use), the chosen password before the marker (a failure
                // in between leaves a pending account), any OTHER password only once the marker is won (an account
                // signed up before email first holds its sign-up password; removed earlier, a concurrent reset that
                // wins would lose its own), and the dead links last.
                events shouldBe listOf(
                    "remove:verification-record",
                    "create:Password",
                    "activate:pending-activation",
                    "remove-passwords:${ownerId.value}-except:new-password",
                    "remove-links",
                    "remove-cancel-links:${ownerId.value}",
                )

                val password = created.filterIsInstance<AuthRecord.Password>().single()
                password.ownerId shouldBe ownerId
                password.realm shouldBe realm.id
                password.token shouldBe "hash(New-1234!)"

                // ... and the marker is the token's OWNER's, not whoever is calling.
                markerLookedUpFor shouldBe (realm.id to ownerId)
            }

            "a leftover link of an ACTIVE account sets NO password and answers null" {
                // Since the link chooses the password, a leftover one would be a password reset nobody asked for.
                // `onCreateAuthRecord` and `onRemovePasswordRecords` keep their exploding defaults.
                val removed = mutableListOf<String>()

                val services = lazy {
                    TestServices(
                        onFindEmailVerificationToken = { realm, token -> linkRecord(realm, token) },
                        onFindPendingActivation = { _, _ -> null },
                        onRemoveAuthRecord = { removed += it; true },
                    )
                }

                subjectWith(services).activateAccount(
                    MinimalTestRealm(onLoadUserById = { storedUser }),
                    AuthActivateAccountRequest(
                        provider = EmailAndPasswordAuth.ID, token = "t", password = "New-1234!",
                        displayName = "Test User",
                    ),
                ) shouldBe null

                withClue("the leftover link is used up") {
                    removed shouldBe listOf("verification-record")
                }
            }

            "a link another submit consumed first goes no further" {
                // `onFindPendingActivation` and every write keep their exploding defaults.
                val services = lazy {
                    TestServices(
                        onFindEmailVerificationToken = { realm, token -> linkRecord(realm, token) },
                        onRemoveAuthRecord = { false },
                    )
                }

                subjectWith(services).activateAccount(
                    MinimalTestRealm(),
                    AuthActivateAccountRequest(
                        provider = EmailAndPasswordAuth.ID, token = "t", password = "New-1234!",
                        displayName = "Test User",
                    ),
                ) shouldBe null
            }

            "a retire, SSO or reset that won the marker meanwhile: exactly the password written here is taken back" {
                // `onRemovePasswordRecords` keeps its exploding default: a lost activation must not touch the winner's
                // password — a reset that won holds one.
                val removed = mutableListOf<String>()

                val services = lazy {
                    TestServices(
                        onFindEmailVerificationToken = { realm, token -> linkRecord(realm, token) },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm) },
                        onRemoveAuthRecord = { removed += it; true },
                        onHashPassword = { "hash" },
                        onCreateAuthRecord = { create -> Stored(_id = "new-password", value = create()) },
                        onRemovePendingActivationIfUnclaimed = { false },
                    )
                }

                subjectWith(services).activateAccount(
                    MinimalTestRealm(onLoadUserById = { storedUser }),
                    AuthActivateAccountRequest(
                        provider = EmailAndPasswordAuth.ID, token = "t", password = "New-1234!",
                        displayName = "Test User",
                    ),
                ) shouldBe null

                removed shouldBe listOf("verification-record", "new-password")
            }
        }

        "signUp again with a PENDING address" - {

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
                claimedBy: String? = null,
            ) = Stored(
                _id = "pending-activation",
                value = AuthRecord.PendingActivation(
                    realm = realm, ownerId = owner, createdAt = createdAt, resends = resends, lastSentAt = lastSentAt,
                    claimedBy = claimedBy,
                ),
            )

            fun subjectWith(services: Lazy<EmailAndPasswordAuth.Services>) = EmailAndPasswordAuth(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                log = NullLog,
                services = services,
            )

            // The existing pending account. `onCreateUserForSignup` keeps its exploding default: signing up again
            // never creates a second account. `getMessaging` explodes too unless a test supplies it.
            fun realmWith(
                tokenConfig: RealmTokenConfig = RealmTokenConfig(),
                getMessaging: (() -> TestMessaging)? = null,
            ) = MinimalTestRealm(
                getDeps = { NullStorageTestDeps(kronos = Kronos.fixed(now)) },
                onLoadUserByEmail = { storedUser },
                getMessaging = getMessaging ?: { TestMessaging() },
                tokenConfig = tokenConfig,
            )

            suspend fun EmailAndPasswordAuth.signUpAgain(realm: MinimalTestRealm) =
                signUp(realm, AuthSignUpRequest.EmailAndPassword(provider = id, email = "user@example.com"))

            "inside the first delay: answered exactly like a fresh sign-up, and NOTHING is sent or reserved" {
                // `onReplacePendingActivation` keeps its exploding default, and so does every mail.
                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onFindPendingActivation = { realm, _ ->
                            pendingMarker(realm, createdAt = now.minus(firstDelay).plus(1.seconds))
                        },
                    )
                }

                val result = subjectWith(services).signUpAgain(realmWith())

                result.user shouldBe storedUser
                result.requiresActivation shouldBe true
            }

            "the delay GROWS: after two re-sends the next one waits the third delay" {
                // One second short of the THIRD delay. With a flat cooldown (the first delay) this would
                // send, so this is the test that fails if the schedule stops growing.
                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onFindPendingActivation = { realm, _ ->
                            pendingMarker(
                                realm, createdAt = now.minus(1.days), resends = 2,
                                lastSentAt = now.minus(thirdDelay).plus(1.seconds),
                            )
                        },
                    )
                }

                subjectWith(services).signUpAgain(realmWith()).requiresActivation shouldBe true
            }

            "a CLAIMED account, or an EXPIRED one the retire did not free: same answer, nothing sent" {
                for (marker in listOf(
                    { realm: RealmId ->
                        pendingMarker(
                            realm, createdAt = now.minus(1.days), claimedBy = AuthRecord.PendingActivation.CLAIMED_BY_SSO,
                        )
                    },
                    { realm: RealmId -> pendingMarker(realm, createdAt = now.minus(config.pendingAccountLifetime)) },
                )) {
                    val services = lazy {
                        TestServices(onInstantNow = { now }, onFindPendingActivation = { realm, _ -> marker(realm) })
                    }

                    subjectWith(services).signUpAgain(realmWith()).requiresActivation shouldBe true
                }
            }

            "an activation that removed the marker meanwhile wins: nothing is re-inserted, nothing is sent" {
                // A save here would re-insert the marker and make the now-ACTIVE account pending again — and
                // the day-7 sweep would then retire it.
                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm, createdAt = now.minus(1.days)) },
                        onReplacePendingActivation = { _, _ -> null }, // the marker is gone
                    )
                }

                subjectWith(services).signUpAgain(realmWith()).requiresActivation shouldBe true
            }

            "once due: reserves the slot BEFORE mailing, rotates the link, and mails a NEW one" {
                val newToken = "the-new-token"
                val generated = ArrayDeque(listOf(newToken, "the-new-cancel-token"))

                val events = mutableListOf<String>()
                var rotation: List<Any>? = null
                var trimmed: List<Any>? = null
                var saved: AuthRecord.PendingActivation? = null
                var activationUrl: String? = null
                var cancelUrl: String? = null
                val createdAuthRecords = mutableListOf<AuthRecord>()

                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onGenerateToken = { generated.removeFirst() },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm, createdAt = now.minus(firstDelay)) },
                        onReplacePendingActivation = { _, marker ->
                            events += "reserve"
                            saved = marker.value()
                            marker
                        },
                        // `onRemoveEmailVerificationTokens` keeps its exploding default: the older links are capped,
                        // not removed.
                        onCapEmailVerificationTokens = { realm, o, exceptId, notAfter ->
                            rotation = listOf(realm, o, exceptId, notAfter)
                        },
                        // Stored hashed, the cancel token cannot be mailed again: every mail mints its own.
                        onTrimSignUpCancelTokens = { realm, o, exceptId, keep -> trimmed = listOf(realm, o, exceptId, keep) },
                        onCreateAuthRecord = { create ->
                            val record = create()
                            createdAuthRecords.add(record)
                            @Suppress("UNCHECKED_CAST")
                            Stored(_id = "record-${createdAuthRecords.size}", value = record)
                        },
                    )
                }

                val realm = realmWith {
                    TestMessaging(
                        onSendAccountActivationEmail = { user, url, cancel ->
                            events += "mail"
                            user shouldBe storedUser
                            activationUrl = url
                            cancelUrl = cancel
                            EmailResult.ofMessageId("message-id")
                        }
                    )
                }

                val result = subjectWith(services).signUpAgain(realm)

                result.user shouldBe storedUser
                result.requiresActivation shouldBe true

                // The slot is taken before the mail goes out, so a failing or slow mail cannot be
                // retried at full speed.
                events shouldBe listOf("reserve", "mail")
                saved?.resends shouldBe 1
                saved?.lastSentAt shouldBe now

                val issued = createdAuthRecords.filterIsInstance<AuthRecord.EmailVerificationToken>().single()
                withClue("the link is stored as its hash, never in clear text") {
                    issued.tokenHash shouldBe TokenHash.of(newToken)
                    issued.token shouldBe null
                }
                issued.ownerId shouldBe owner

                // Rotation must EXCLUDE the row just created. Rotating first (or without the exception)
                // lets two interleaved re-sends kill the token the other has already mailed, so a user
                // receives a link that was dead before it arrived. The older links live on for the overlap.
                rotation shouldBe listOf(realm.id, owner, "record-1", now.plus(config.activationLinkOverlap))

                // The re-sent links must be built exactly like the sign-up ones.
                activationUrl shouldBe "https://a.b.c/auth/${EmailAndPasswordAuth.ID}/activate/$newToken"
                cancelUrl shouldBe "https://a.b.c/auth/${EmailAndPasswordAuth.ID}/cancel-signup/the-new-cancel-token"
                withClue("a cancel token of its own, stored as its hash only; the older ones trimmed to the cap") {
                    val cancel = createdAuthRecords.filterIsInstance<AuthRecord.SignUpCancelToken>().single()
                    cancel.tokenHash shouldBe TokenHash.of("the-new-cancel-token")
                    cancel.token shouldBe null
                    trimmed shouldBe listOf(realm.id, owner, "record-2", config.maxSignUpCancelTokens)
                }
                withClue("no password is ever stored") {
                    createdAuthRecords.filterIsInstance<AuthRecord.Password>() shouldBe emptyList()
                }
            }

            "with NO overlap, the older links are removed at once" {
                var removed: Triple<RealmId, UserId, String?>? = null

                val services = lazy {
                    TestServices(
                        onInstantNow = { now },
                        onGenerateToken = { "token" },
                        onFindPendingActivation = { realm, _ -> pendingMarker(realm, createdAt = now.minus(firstDelay)) },
                        onReplacePendingActivation = { _, next -> next },
                        // `onCapEmailVerificationTokens` keeps its exploding default.
                        onRemoveEmailVerificationTokens = { realm, o, exceptId -> removed = Triple(realm, o, exceptId) },
                        onTrimSignUpCancelTokens = { _, _, _, _ -> },
                        onCreateAuthRecord = { create -> Stored(_id = "record", value = create()) },
                    )
                }

                val realm = realmWith(tokenConfig = RealmTokenConfig(activationLinkOverlap = Duration.ZERO)) {
                    TestMessaging(onSendAccountActivationEmail = { _, _, _ -> EmailResult.ofMessageId("id") })
                }

                subjectWith(services).signUpAgain(realm).requiresActivation shouldBe true

                removed shouldBe Triple(realm.id, owner, "record")
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
                        onReplacePendingActivation = { _, next -> next },
                        onCapEmailVerificationTokens = { _, _, _, _ -> },
                        onTrimSignUpCancelTokens = { _, _, _, _ -> },
                        onCreateAuthRecord = { create ->
                            val record = create()
                            createdAuthRecords.add(record)
                            Stored(_id = "record", value = record)
                        },
                    )
                }

                subjectWith(services).signUpAgain(
                    realmWith { TestMessaging(onSendAccountActivationEmail = { _, _, _ -> EmailResult.ofMessageId("id") }) }
                ).requiresActivation shouldBe true

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
                                    realm = realm, ownerId = UserId("user-id"), expiresAt = Long.MAX_VALUE, tokenHash = TokenHash.of(token),
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
                                    tokenHash = TokenHash.of(token),
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
                        onRemovePasswordRecords = { _, owner, exceptId -> removedIds += "passwords-of:${owner.value}-except:$exceptId" },
                        onRemoveEmailVerificationTokens = { _, owner, _ -> removedIds += "links-of:${owner.value}" },
                        onRemoveSignUpCancelTokens = { _, owner -> removedIds += "cancel-links-of:${owner.value}" },
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
                // ... and, having activated, the account's other passwords go: one signed up before email first may
                // be a pre-registering attacker's.
                removedIds shouldBe listOf(
                    "recovery-record", "pending-activation", "passwords-of:${ownerId.value}-except:new-password-record",
                    // ... and the sign-up's links, as after an activation (ULTRA-00291).
                    "links-of:${ownerId.value}", "cancel-links-of:${ownerId.value}",
                )
            }

            "on an ACTIVE (non-retired) account — no marker — sets the password" {
                // The most common reset in production; the retired-owner guard must not catch it.
                val storedUser = Stored(_id = "user-id", value = MinimalTestUser())
                val created = mutableListOf<AuthRecord>()

                val services = lazy {
                    TestServices(
                        onHashPassword = { "hashed-password" },
                        onFindPasswordRecoveryToken = { realm, token ->
                            Stored(
                                _id = "recovery-record",
                                value = AuthRecord.PasswordRecoveryToken(
                                    realm = realm, ownerId = UserId(storedUser._id), tokenHash = TokenHash.of(token), expiresAt = Long.MAX_VALUE,
                                ),
                            )
                        },
                        onRemoveAuthRecord = { true },
                        onFindPendingActivation = { _, _ -> null },
                        // `removePendingActivationIfUnclaimed` keeps its exploding default: there is no marker.
                        onCreateAuthRecord = { create -> create().let { created += it; Stored(_id = "new-password-record", value = it) } },
                    )
                }

                val subject = EmailAndPasswordAuth(
                    capabilities = setOf(AuthProviderModel.Capability.SignIn),
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                    log = NullLog,
                    services = services,
                )

                subject.recoverAccountSetPasswordWithToken(
                    MinimalTestRealm(
                        onLoadUserById = { storedUser },
                        getMessaging = { TestMessaging(onSendPasswordChangedEmail = { EmailResult.ofMessageId("message-id") }) },
                    ),
                    AuthRecoverAccountRequest.SetPasswordWithToken(
                        provider = subject.id, token = "the-recovery-token", password = "A-valid-password-123!",
                    ),
                ).success shouldBe true

                created.map { it::class } shouldBe listOf(AuthRecord.Password::class)
            }
        }

        "recoverAccountSetPasswordWithToken on a pending account that is not plainly live" - {

            fun recoveryToken(realm: RealmId) = Stored(
                _id = "recovery-record",
                value = AuthRecord.PasswordRecoveryToken(
                    realm = realm, ownerId = UserId("user-id"), tokenHash = TokenHash.of("t"), expiresAt = Long.MAX_VALUE,
                ),
            )

            fun subjectWith(services: Lazy<EmailAndPasswordAuth.Services>) = EmailAndPasswordAuth(
                frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c/auth"),
                log = NullLog,
                services = services,
            )

            val request = AuthRecoverAccountRequest.SetPasswordWithToken(provider = "email-password", token = "t", password = "Pw-123456!")

            "a WEAK password is refused before the token is even looked up — the link stays usable" {
                // Every service keeps its exploding default. A reset also activates a pending account, so it chooses
                // a first password as much as the activation page does (red-team ULTRA-00127 §7: only the frontend
                // checked).
                shouldThrow<AuthError> {
                    subjectWith(lazy { TestServices() }).recoverAccountSetPasswordWithToken(
                        MinimalTestRealm(),
                        request.copy(password = "weak"),
                    )
                }.message shouldBe "Weak password"
            }

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

            "a NEW password over 256 characters is a weak password; a CURRENT one is refused before any hashing" {
                // ULTRA-00296. The fakes' defaults throw: nothing may be hashed, checked or looked up.
                val subject = EmailAndPasswordAuth(
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = lazy { TestServices() },
                )
                val user = Stored(_id = "user-id", value = MinimalTestUser())
                val strong = "Aa1!" + "x".repeat(253) // 257

                shouldThrow<AuthError> {
                    subject.setPassword(
                        MinimalTestRealm(passwordPolicy = PasswordPolicy.default),
                        AuthSetPasswordRequest(subject.id, UserId(user._id), currentPassword = "old", newPassword = strong),
                    )
                }.message shouldBe "Weak password"

                shouldThrow<AuthError> {
                    subject.setPassword(
                        MinimalTestRealm(passwordPolicy = PasswordPolicy.default, onLoadUserById = { user }),
                        AuthSetPasswordRequest(
                            subject.id, UserId(user._id), currentPassword = "x".repeat(257), newPassword = "A-valid-password-123!",
                        ),
                    )
                }.message shouldBe "Invalid credentials"
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
