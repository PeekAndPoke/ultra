package io.peekandpoke.funktor.auth.provider

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.json.webtoken.JsonWebSignature
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.AccountRetirement
import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.FunktorAuthBuilder
import io.peekandpoke.funktor.auth.MinimalTestRealm
import io.peekandpoke.funktor.auth.MinimalTestUser
import io.peekandpoke.funktor.auth.RealmTokenConfig
import io.peekandpoke.funktor.auth.createAuthTestContainer
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.funktorAuth
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class SsoPendingAccountKarangoSpec : SsoPendingAccountBaseSpec() {
    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useKarango()
    }
}

class SsoPendingAccountMonkoSpec : SsoPendingAccountBaseSpec() {
    override fun KontainerBuilder.configureKontainer() {
        monko(MongoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useMonko()
    }
}

/**
 * Decision 2 (2026-10-19), against a REAL auth-record store: SSO on a PENDING password account activates it
 * and deletes the sign-up password when the provider says the address is verified, and is refused when it
 * does not. An ACTIVE account, or a new one, is reached only with a verified address too (ULTRA-00118).
 */
abstract class SsoPendingAccountBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()

    abstract fun FunktorAuthBuilder.configureAuth()

    private val realmId = RealmId("test-realm")
    private val email = EmailAddress.of("victim@example.com")
    private val user = Stored(_id = "test_users/victim", value = MinimalTestUser(email = email))
    private val owner = UserId(user._id)

    private suspend fun realDeps(): AuthSystem.Deps = createAuthTestContainer(
        configureKontainer = { configureKontainer() },
        configureAuth = { configureAuth() },
    ).funktorAuth.deps.also { it.storage.authRecords.adapter.removeAll() }

    private fun realmWith(deps: AuthSystem.Deps) = MinimalTestRealm(
        getDeps = { deps },
        onLoadUserByEmail = { if (it == email) user else null },
    )

    /** A realm without the victim's account, that counts the accounts a sign-up creates. */
    private class EmptyRealm(deps: AuthSystem.Deps) {
        var created = 0

        val realm = MinimalTestRealm(
            getDeps = { deps },
            onLoadUserByEmail = { null },
            onCreateUserForSignup = { params ->
                created++
                Stored(_id = "test_users/new", value = MinimalTestUser(email = params.email))
            },
        )
    }

    /** What a password sign-up leaves behind, plus a resend token an attacker would hold. */
    private suspend fun AuthRecordStorage.preRegister(pending: Boolean) {
        val expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds()

        create(AuthRecord.Password(realm = realmId, ownerId = owner, token = "attacker-hash"))
        create(AuthRecord.EmailVerificationToken(realmId, owner, expiresAt, token = "verify"))
        create(AuthRecord.ActivationResendToken(realmId, owner, expiresAt, token = "resend"))

        if (pending) create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))
    }

    private suspend fun AuthRecordStorage.state() = mapOf(
        "password" to (findLatestRecordBy(AuthRecord.Password, realmId, owner) != null),
        "marker" to (findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner) != null),
        "verification" to (findLatestRecordBy(AuthRecord.EmailVerificationToken, realmId, owner) != null),
        "resend" to (findLatestRecordBy(AuthRecord.ActivationResendToken, realmId, owner) != null),
    )

    private val untouched = mapOf("password" to true, "marker" to true, "verification" to true, "resend" to true)
    private val activated = mapOf("password" to false, "marker" to false, "verification" to false, "resend" to false)

    //  Google  //////////////////////////////////////////////////////////////////////////////////////////////////

    private fun google(verified: Boolean?) = GoogleSsoAuth(
        capabilities = setOf(AuthProviderModel.Capability.SignIn),
        clientId = "irrelevant",
        remoteClient = lazy {
            object : GoogleSsoAuth.RemoteClient {
                override suspend fun verifyIdToken(token: String): GoogleIdToken = GoogleIdToken(
                    JsonWebSignature.Header(),
                    GoogleIdToken.Payload().apply {
                        this.email = "Victim@Example.com"
                        this.emailVerified = verified
                    },
                    byteArrayOf(),
                    byteArrayOf(),
                )
            }
        },
    )

    private fun ssoRequest(provider: AuthProvider) = AuthSignInRequest.OAuth(provider = provider.id, token = "t")

    private fun ssoSignUp(provider: AuthProvider) = AuthSignUpRequest.OAuth(provider = provider.id, token = "t")

    //  GitHub  //////////////////////////////////////////////////////////////////////////////////////////////////

    private class FakeGithub(val emails: JsonArray?) : GithubSsoAuth.RemoteClient {
        var emailsAsked = 0

        override suspend fun getAccessToken(code: String) = buildJsonObject { put("access_token", "a") }

        override suspend fun getUser(access: JsonObject) = buildJsonObject { put("email", "victim@example.com") }

        override suspend fun getEmails(access: JsonObject): JsonArray? = emails.also { emailsAsked++ }
    }

    private fun githubEmails(vararg entries: Triple<String, Boolean, Boolean>) = buildJsonArray {
        entries.forEach { (address, primary, verified) ->
            add(buildJsonObject {
                put("email", address)
                put("primary", primary)
                put("verified", verified)
            })
        }
    }

    private fun github(remote: FakeGithub) = GithubSsoAuth(
        capabilities = setOf(AuthProviderModel.Capability.SignIn),
        clientId = "irrelevant",
        clientSecret = "irrelevant",
        remoteClient = lazy { remote },
    )

    init {
        "Google" - {
            "email_verified = true on a PENDING account activates it and deletes the sign-up password" {
                val deps = realDeps()
                deps.storage.authRecords.preRegister(pending = true)
                val subject = google(verified = true)

                subject.signIn(realmWith(deps), ssoRequest(subject)) shouldBe user

                deps.storage.authRecords.state() shouldBe activated
            }

            "email_verified = false or absent on a PENDING account is refused, and changes nothing" {
                listOf(false, null).forEach { claim ->
                    withClue("email_verified = $claim") {
                        val deps = realDeps()
                        deps.storage.authRecords.preRegister(pending = true)
                        val subject = google(verified = claim)

                        shouldThrow<AuthError> {
                            subject.signIn(realmWith(deps), ssoRequest(subject))
                        }.message shouldBe "Invalid credentials"

                        deps.storage.authRecords.state() shouldBe untouched
                    }
                }
            }

            "email_verified = true on an ACTIVE account signs in and up, and keeps its password" {
                val deps = realDeps()
                deps.storage.authRecords.preRegister(pending = false)
                val subject = google(verified = true)

                subject.signIn(realmWith(deps), ssoRequest(subject)) shouldBe user
                subject.signUp(realmWith(deps), ssoSignUp(subject)).user shouldBe user

                deps.storage.authRecords.state() shouldBe untouched.plus("marker" to false)
            }

            "email_verified = false or absent on an ACTIVE account is refused, sign-in and sign-up (ULTRA-00118)" {
                // The takeover: a provider account that claims the victim's address without verifying it
                listOf(false, null).forEach { claim ->
                    withClue("email_verified = $claim") {
                        val deps = realDeps()
                        deps.storage.authRecords.preRegister(pending = false)
                        val subject = google(verified = claim)

                        shouldThrow<AuthError> {
                            subject.signIn(realmWith(deps), ssoRequest(subject))
                        }.message shouldBe "Invalid credentials"

                        shouldThrow<AuthError> {
                            subject.signUp(realmWith(deps), ssoSignUp(subject))
                        }.message shouldBe "Invalid credentials"

                        deps.storage.authRecords.state() shouldBe untouched.plus("marker" to false)
                    }
                }
            }

            "a sign-up creates an account only for a verified address" {
                listOf(false, null).forEach { claim ->
                    withClue("email_verified = $claim") {
                        val empty = EmptyRealm(realDeps())
                        val subject = google(verified = claim)

                        shouldThrow<AuthError> {
                            subject.signUp(empty.realm, ssoSignUp(subject))
                        }.message shouldBe "Invalid credentials"

                        empty.created shouldBe 0
                    }
                }

                val empty = EmptyRealm(realDeps())
                val subject = google(verified = true)

                subject.signUp(empty.realm, ssoSignUp(subject)).user.value.email shouldBe email
                empty.created shouldBe 1
            }

            "an EXPIRED pending account is retired, not activated — even with a verified address" {
                val deps = realDeps()
                deps.storage.authRecords.preRegister(pending = true)
                var retired = false

                val realm = MinimalTestRealm(
                    getDeps = { deps },
                    tokenConfig = RealmTokenConfig(pendingAccountLifetime = Duration.ZERO),
                    onLoadUserById = { user },
                    onLoadUserByEmail = { if (it == email && !retired) user else null },
                    onRetire = { u, _ -> retired = true; u },
                )
                val subject = google(verified = true)

                shouldThrow<AuthError> { subject.signIn(realm, ssoRequest(subject)) }.message shouldBe "Invalid credentials"

                retired shouldBe true
                deps.storage.authRecords.state() shouldBe activated
            }

            "a sign-UP on an EXPIRED pending account retires it, then creates a fresh one only for a verified address" {
                listOf(true to 1, false to 0, null to 0).forEach { (claim, accounts) ->
                    withClue("email_verified = $claim") {
                        val deps = realDeps()
                        deps.storage.authRecords.preRegister(pending = true)
                        var retired = false
                        var created = 0

                        val realm = MinimalTestRealm(
                            getDeps = { deps },
                            tokenConfig = RealmTokenConfig(pendingAccountLifetime = Duration.ZERO),
                            onLoadUserById = { user },
                            onLoadUserByEmail = { if (it == email && !retired) user else null },
                            onRetire = { u, _ -> retired = true; u },
                            onCreateUserForSignup = { params ->
                                created++
                                Stored(_id = "test_users/fresh", value = MinimalTestUser(email = params.email))
                            },
                        )
                        val subject = google(verified = claim)

                        when (claim) {
                            true -> subject.signUp(realm, ssoSignUp(subject)).user._id shouldBe "test_users/fresh"
                            else -> shouldThrow<AuthError> {
                                subject.signUp(realm, ssoSignUp(subject))
                            }.message shouldBe "Invalid credentials"
                        }

                        retired shouldBe true
                        created shouldBe accounts
                    }
                }
            }

            "a RETIRED address is refused, whatever the provider says" {
                val deps = realDeps()
                val retiredUser = Stored(
                    _id = user._id,
                    value = MinimalTestUser(email = AccountRetirement.of(owner).rename(email)),
                )
                val realm = MinimalTestRealm(getDeps = { deps })

                shouldThrow<AuthError> {
                    realm.resolveSsoAccount(retiredUser, provider = "test") { true }
                }.message shouldBe "Invalid credentials"
            }

            "a retire that wins the marker meanwhile leaves nothing to sign into" {
                val deps = realDeps()
                deps.storage.authRecords.preRegister(pending = true)
                val realm = realmWith(deps)

                shouldThrow<AuthError> {
                    realm.resolveSsoAccount(user, provider = "test") {
                        // A concurrent retire removes the marker while the provider is asked.
                        deps.storage.authRecords.removeAllByOwner(AuthRecord.PendingActivation, realmId, owner)
                        true
                    }
                }.message shouldBe "Invalid credentials"
            }

            "CLAIM FIRST: a retire that claims the account while the provider is asked wins, and NOTHING is deleted" {
                val deps = realDeps()
                deps.storage.authRecords.preRegister(pending = true)
                val realm = realmWith(deps)

                shouldThrow<AuthError> {
                    realm.resolveSsoAccount(user, provider = "test") {
                        val marker = deps.storage.authRecords
                            .findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner).shouldNotBeNull()
                        deps.storage.authRecords.replaceIfUnclaimed(
                            marker.modify { it.copy(claimedBy = AuthRecord.PendingActivation.CLAIMED_BY_RETIRE) }
                        ).shouldNotBeNull()
                        true
                    }
                }.message shouldBe "Invalid credentials"

                withClue("the SSO lost the claim, so the password and tokens are the retire's to remove — untouched") {
                    deps.storage.authRecords.state() shouldBe untouched
                }
            }

            "the password goes BEFORE the marker: a failure in between leaves the account pending, never live" {
                // If the marker went first and the password delete failed, the pre-registering attacker's
                // password would start working on an account the victim now uses.
                val deps = realDeps()
                deps.storage.authRecords.preRegister(pending = true)
                val failing = depsFailingOn(deps, AuthRecord.Password.identifier)
                val subject = google(verified = true)

                shouldThrow<IllegalStateException> {
                    subject.signIn(realmWith(failing), ssoRequest(subject))
                }

                deps.storage.authRecords.state()["marker"] shouldBe true
            }
        }

        "GitHub" - {
            "a verified address from /user/emails activates a pending account, primary or not" {
                listOf(true, false).forEach { primary ->
                    withClue("primary = $primary") {
                        val deps = realDeps()
                        deps.storage.authRecords.preRegister(pending = true)
                        val subject = github(FakeGithub(githubEmails(Triple("Victim@example.com", primary, true))))

                        subject.signIn(realmWith(deps), ssoRequest(subject)) shouldBe user

                        deps.storage.authRecords.state() shouldBe activated
                    }
                }
            }

            "anything less is refused on a pending account, and changes nothing" {
                val cases = mapOf(
                    "primary but not verified" to githubEmails(Triple("victim@example.com", true, false)),
                    "a DIFFERENT address is verified" to githubEmails(Triple("other@example.com", true, true)),
                    "no emails at all" to buildJsonArray { },
                    "garbage entries" to buildJsonArray { add(JsonPrimitive("victim@example.com")) },
                )

                (cases + ("the emails API failed" to null)).forEach { (case, emails) ->
                    withClue(case) {
                        val deps = realDeps()
                        deps.storage.authRecords.preRegister(pending = true)
                        val subject = github(FakeGithub(emails))

                        shouldThrow<AuthError> {
                            subject.signIn(realmWith(deps), ssoRequest(subject))
                        }.message shouldBe "Invalid credentials"

                        deps.storage.authRecords.state() shouldBe untouched
                    }
                }
            }

            "a verified address signs in and up to an ACTIVE account, primary or not, asking /user/emails once each" {
                // A user's public /user address need not be their primary; refusing it would lock them out
                listOf(true, false).forEach { primary ->
                    withClue("primary = $primary") {
                        val deps = realDeps()
                        deps.storage.authRecords.preRegister(pending = false)
                        val remote = FakeGithub(githubEmails(Triple("victim@example.com", primary, true)))
                        val subject = github(remote)

                        subject.signIn(realmWith(deps), ssoRequest(subject)) shouldBe user
                        remote.emailsAsked shouldBe 1

                        subject.signUp(realmWith(deps), ssoSignUp(subject)).user shouldBe user
                        remote.emailsAsked shouldBe 2

                        deps.storage.authRecords.state()["password"] shouldBe true
                    }
                }
            }

            "anything less is refused on an ACTIVE account, sign-in and sign-up (ULTRA-00118)" {
                val cases = mapOf(
                    "primary but not verified" to githubEmails(Triple("victim@example.com", true, false)),
                    "a DIFFERENT address is verified" to githubEmails(Triple("other@example.com", true, true)),
                    "the emails API failed" to null,
                )

                cases.forEach { (case, emails) ->
                    withClue(case) {
                        val deps = realDeps()
                        deps.storage.authRecords.preRegister(pending = false)
                        val subject = github(FakeGithub(emails))

                        shouldThrow<AuthError> {
                            subject.signIn(realmWith(deps), ssoRequest(subject))
                        }.message shouldBe "Invalid credentials"

                        shouldThrow<AuthError> {
                            subject.signUp(realmWith(deps), ssoSignUp(subject))
                        }.message shouldBe "Invalid credentials"

                        deps.storage.authRecords.state()["password"] shouldBe true
                    }
                }
            }

            "a sign-up creates an account only for a verified address, asking /user/emails once" {
                val refused = EmptyRealm(realDeps())
                val unverified = github(FakeGithub(githubEmails(Triple("victim@example.com", true, false))))

                shouldThrow<AuthError> {
                    unverified.signUp(refused.realm, ssoSignUp(unverified))
                }.message shouldBe "Invalid credentials"

                refused.created shouldBe 0

                val empty = EmptyRealm(realDeps())
                val remote = FakeGithub(githubEmails(Triple("victim@example.com", false, true)))
                val verified = github(remote)

                verified.signUp(empty.realm, ssoSignUp(verified)).user.value.email shouldBe email
                empty.created shouldBe 1
                remote.emailsAsked shouldBe 1
            }
        }
    }

    /** [deps] whose auth-record store throws when asked to remove records of type [failOnType]. */
    private fun depsFailingOn(deps: AuthSystem.Deps, failOnType: String): AuthSystem.Deps {
        val real = deps.storage.authRecords

        val failing = object : AuthRecordStorage.Adapter by real.adapter {
            override suspend fun removeAllByOwner(
                realm: RealmId, type: String, owner: UserId, exceptId: String?,
            ): RemoveResult {
                check(type != failOnType) { "injected failure removing $type" }
                return real.adapter.removeAllByOwner(realm, type, owner, exceptId)
            }
        }

        val storage = AuthSystem.Storage(lazy { AuthRecordStorage(lazy { failing }, lazy { real.kronos }) })

        return object : AuthSystem.Deps by deps {
            override val storage: AuthSystem.Storage = storage
        }
    }
}
