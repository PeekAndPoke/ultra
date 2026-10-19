package io.peekandpoke.funktor.auth

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Stored
import kotlin.time.Duration.Companion.hours

class AuthRealmActivationKarangoSpec : AuthRealmActivationBaseSpec() {
    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useKarango()
    }
}

class AuthRealmActivationMonkoSpec : AuthRealmActivationBaseSpec() {
    override fun KontainerBuilder.configureKontainer() {
        monko(MongoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useMonko()
    }
}

/**
 * The realm's own share of activation, against a REAL auth-record store: consuming the resend token, and
 * the guards around retiring an account. The happy paths run end to end in `ActivationHardeningE2eSpec`.
 */
abstract class AuthRealmActivationBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()

    abstract fun FunktorAuthBuilder.configureAuth()

    private val realmId = RealmId("test-realm")
    private val email = EmailAddress.of("pending@example.com")
    private val user = Stored(_id = "test_users/pending", value = MinimalTestUser(email = email))
    private val owner = UserId(user._id)

    private suspend fun realDeps(): AuthSystem.Deps = createAuthTestContainer(
        configureKontainer = { configureKontainer() },
        configureAuth = { configureAuth() },
    ).funktorAuth.deps.also { it.storage.authRecords.adapter.removeAll() }

    /** A provider that fails the test the moment a resend reaches it. */
    private val unreachableProvider = EmailAndPasswordAuth(
        frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
        log = NullLog,
        services = lazy { error("the provider must not be reached") },
    )

    init {
        "resendActivation" - {

            "a token another request already consumed sends nothing — the delete COUNT decides" {
                // Two requests found the token; the other one's delete won. Only a store that reports
                // what THIS delete removed can stop the second from going on to the provider.
                val real = realDeps()
                real.storage.authRecords.create(
                    AuthRecord.ActivationResendToken(
                        realm = realmId,
                        ownerId = owner,
                        token = "resend",
                        expiresAt = real.kronos.instantNow().plus(1.hours).toEpochSeconds(),
                    )
                )

                val lostTheRace = depsWhereRemoveById(real) { id ->
                    real.storage.authRecords.adapter.removeById(id) // the other request's delete
                    real.storage.authRecords.adapter.removeById(id) // ours: removes nothing
                }

                val realm = MinimalTestRealm(getDeps = { lostTheRace }, getProviders = { listOf(unreachableProvider) })

                realm.resendActivation(
                    AuthResendActivationRequest(provider = EmailAndPasswordAuth.ID, token = "resend"),
                ).sent shouldBe false

                real.storage.authRecords.findByToken(AuthRecord.ActivationResendToken, realmId, "resend").shouldBeNull()
            }

            "an unknown token sends nothing and never reaches the provider" {
                val realm = MinimalTestRealm(getDeps = { NullStorageTestDeps() }, getProviders = { listOf(unreachableProvider) })

                realm.resendActivation(
                    AuthResendActivationRequest(provider = EmailAndPasswordAuth.ID, token = "nope"),
                ).sent shouldBe false
            }
        }

        "retirePendingAccount" - {

            "an ACTIVATED account is never retired — nothing is renamed or removed" {
                val real = realDeps()
                real.storage.authRecords.create(AuthRecord.Password(realm = realmId, ownerId = owner, token = "pw"))

                // `onRetire` keeps its exploding default.
                val realm = MinimalTestRealm(getDeps = { real }, onLoadUserById = { user })

                realm.retirePendingAccount(owner) shouldBe false

                real.storage.authRecords.findLatestRecordBy(AuthRecord.Password, realmId, owner).shouldNotBeNull()
            }

            "a realm whose retire() renames NOTHING is caught: the account stays pending" {
                // Otherwise the address would stay taken forever while the account looked retired.
                val real = realDeps()
                real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))

                val realm = MinimalTestRealm(
                    getDeps = { real },
                    onLoadUserById = { user },
                    onLoadUserByEmail = { if (it == email) user else null },
                    onRetire = { u, _ -> u }, // a no-op "retire"
                )

                realm.retirePendingAccount(owner) shouldBe false

                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner)
                    .shouldNotBeNull()
            }

            "a realm whose retire() THROWS leaves the account pending, and retiring again later works" {
                val real = realDeps()
                real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))

                var renamedTo: EmailAddress? = null
                var failNext = true

                val realm = MinimalTestRealm(
                    getDeps = { real },
                    onLoadUserById = { user },
                    onLoadUserByEmail = { if (it == email && renamedTo == null) user else null },
                    onRetire = { u, retirement ->
                        check(!failNext) { "the user store is down" }
                        renamedTo = retirement.rename(u.value().email)
                        u
                    },
                )

                realm.retirePendingAccount(owner) shouldBe false
                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner)
                    .shouldNotBeNull()

                failNext = false

                realm.retirePendingAccount(owner) shouldBe true
                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner).shouldBeNull()
                AccountRetirement.isRetired(renamedTo.shouldNotBeNull()) shouldBe true
            }
        }
    }

    /** [deps] whose `removeById` is replaced by [onRemoveById]. */
    private fun depsWhereRemoveById(deps: AuthSystem.Deps, onRemoveById: suspend (String) -> RemoveResult): AuthSystem.Deps {
        val real = deps.storage.authRecords

        val adapter = object : AuthRecordStorage.Adapter by real.adapter {
            override suspend fun removeById(id: String): RemoveResult = onRemoveById(id)
        }

        val storage = AuthSystem.Storage(lazy { AuthRecordStorage(lazy { adapter }, lazy { real.kronos }) })

        return object : AuthSystem.Deps by deps {
            override val storage: AuthSystem.Storage = storage
        }
    }
}
