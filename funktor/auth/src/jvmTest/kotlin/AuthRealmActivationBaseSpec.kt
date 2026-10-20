package io.peekandpoke.funktor.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
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
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

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

    init {
        "retirePendingAccount: CLAIM FIRST" - {

            "an activation that lost to the claim changes nothing, and the retire completes" {
                val real = realDeps()
                val marker = real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))
                var renamedTo: EmailAddress? = null
                var activationWon: Boolean? = null

                val realm = MinimalTestRealm(
                    getDeps = { real },
                    onLoadUserById = { user },
                    onLoadUserByEmail = { if (it == email && renamedTo == null) user else null },
                    onRetire = { u, retirement ->
                        // A concurrent activation lands while the realm renames: its conditional delete must lose.
                        activationWon = real.storage.authRecords.removeIfUnclaimed(marker._id)
                        renamedTo = retirement.rename(u.value().email)
                        u
                    },
                )

                realm.retirePendingAccount(owner) shouldBe true
                activationWon shouldBe false
                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner).shouldBeNull()
            }

            "an activation that READ the marker before a retire claimed it cannot commit after — the delete is conditional" {
                // The one interleaving only the conditional delete stops: the activation's read saw an unclaimed,
                // fresh marker (so neither the claim check nor the expiry check fires), the retire's claim lands,
                // then the activation deletes. Simulated with a store that serves the activation the stale read.
                val real = realDeps()
                val marker = real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))
                real.storage.authRecords.create(
                    AuthRecord.EmailVerificationToken(
                        realmId, owner, expiresAt = real.kronos.instantNow().plus(1.hours).toEpochSeconds(), token = "act",
                    )
                )

                real.storage.authRecords.replaceIfUnclaimed(
                    marker.modify { it.copy(claimedBy = AuthRecord.PendingActivation.CLAIMED_BY_RETIRE) }
                ).shouldNotBeNull()

                val staleRead = depsWhereMarkerReadIs(real, marker)
                val provider = EmailAndPasswordAuth(
                    frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
                    log = NullLog,
                    services = lazy { EmailAndPasswordAuth.Services(staleRead) },
                )

                provider.activateAccount(
                    MinimalTestRealm(getDeps = { staleRead }),
                    AuthActivateAccountRequest(provider = provider.id, token = "act"),
                ).success shouldBe false

                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner)
                    .shouldNotBeNull().value().claimedBy shouldBe AuthRecord.PendingActivation.CLAIMED_BY_RETIRE
            }

            "a retire that LOST the claim to an activation changes NOTHING — credentials and address stay" {
                val real = realDeps()
                real.storage.authRecords.create(AuthRecord.Password(realm = realmId, ownerId = owner, token = "pw"))
                val marker = real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))

                // The activation committed between the retire's read and its claim: simulated by a store whose
                // claim write finds the marker gone.
                val activatedMeanwhile = depsWhereReplaceIfUnclaimed(real) {
                    real.storage.authRecords.removeIfUnclaimed(marker._id) shouldBe true
                    null
                }

                // `onRetire` keeps its exploding default: nothing may be renamed.
                val realm = MinimalTestRealm(getDeps = { activatedMeanwhile }, onLoadUserById = { user })

                realm.retirePendingAccount(owner) shouldBe false
                real.storage.authRecords.findLatestRecordBy(AuthRecord.Password, realmId, owner).shouldNotBeNull()
            }

            "a retire that stops part-way stays CLAIMED, blocked and swept: the next sweep finishes it" {
                val real = realDeps()
                real.storage.authRecords.create(AuthRecord.Password(realm = realmId, ownerId = owner, token = "pw"))
                real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))
                var storeDown = true
                var renamedTo: EmailAddress? = null

                // A FRESH account and the default 7-day lifetime: only the claim can make the sweep pick it up.
                val realm = MinimalTestRealm(
                    getDeps = { real },
                    onLoadUserById = { user },
                    onLoadUserByEmail = { if (it == email && renamedTo == null) user else null },
                    onRetire = { u, retirement ->
                        check(!storeDown) { "the user store is down" }
                        renamedTo = retirement.rename(u.value().email)
                        u
                    },
                )

                realm.retirePendingAccount(owner) shouldBe false

                val claimed = real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner)
                    .shouldNotBeNull()
                claimed.value().claimedBy shouldBe AuthRecord.PendingActivation.CLAIMED_BY_RETIRE
                real.storage.authRecords.findLatestRecordBy(AuthRecord.Password, realmId, owner).shouldBeNull()
                withClue("an activation cannot take a claimed marker") {
                    real.storage.authRecords.removeIfUnclaimed(claimed._id) shouldBe false
                }
                withClue("refresh refuses it") {
                    shouldThrow<AuthError> { realm.refreshToken(owner, expectedUserType = null, currentOrgId = null) }
                }

                storeDown = false
                realm.retireExpiredPendingAccounts() shouldBe 1

                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner).shouldBeNull()
                AccountRetirement.isRetired(renamedTo.shouldNotBeNull()) shouldBe true
            }
        }

        "retireExpiredPendingAccounts" - {

            val expired = RealmTokenConfig(pendingAccountLifetime = Duration.ZERO)

            "an account that cannot be retired does not hold up the rest, across pages" {
                val real = realDeps()
                val owners = listOf("test_users/a", "test_users/b", "test_users/c").map { UserId(it) }
                owners.forEach {
                    real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = it))
                    delay(5.milliseconds)
                }

                val renamed = mutableSetOf<String>()

                val realm = MinimalTestRealm(
                    getDeps = { real },
                    tokenConfig = expired,
                    // The OLDEST one cannot even be loaded; the next one cannot be renamed.
                    onLoadUserById = { id ->
                        check(id != owners[0]) { "the user store is down for ${id.value}" }
                        Stored(_id = id.value, value = MinimalTestUser(email = EmailAddress.of("${id.value.substringAfter("/")}@x.com")))
                    },
                    onLoadUserByEmail = { null },
                    onRetire = { u, retirement ->
                        check(u._id != owners[1].value) { "cannot rename ${u._id}" }
                        renamed += u._id
                        u.modify { it.copy(email = retirement.rename(it.email)) }
                    },
                )

                realm.retireExpiredPendingAccounts(pageSize = 1) shouldBe 1

                renamed shouldBe setOf(owners[2].value)
                withClue("the failures stay pending, to be tried again by the next run") {
                    real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owners[0]).shouldNotBeNull()
                    real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owners[1]).shouldNotBeNull()
                    real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owners[2]).shouldBeNull()
                }
            }

            "a realm whose retire() throws an AuthError (not an Exception) does not stop the sweep" {
                // `AuthError` is a Throwable; the adapter KDoc tells realms without sign-up to throw notSupported().
                val real = realDeps()
                val owners = listOf("test_users/x", "test_users/y").map { UserId(it) }
                owners.forEach {
                    real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = it))
                    delay(5.milliseconds)
                }

                val realm = MinimalTestRealm(
                    getDeps = { real },
                    tokenConfig = expired,
                    onLoadUserById = { id -> Stored(_id = id.value, value = MinimalTestUser(email = EmailAddress.of("${id.value.substringAfter("/")}@x.com"))) },
                    onLoadUserByEmail = { null },
                    onRetire = { u, retirement ->
                        if (u._id == owners[0].value) throw AuthError.notSupported()
                        u.modify { it.copy(email = retirement.rename(it.email)) }
                    },
                )

                realm.retireExpiredPendingAccounts() shouldBe 1
                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owners[1]).shouldBeNull()
            }

            "cancellation is NOT swallowed" {
                val real = realDeps()
                real.storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))

                val realm = MinimalTestRealm(
                    getDeps = { real },
                    tokenConfig = expired,
                    onLoadUserById = { user },
                    onRetire = { _, _ -> throw CancellationException("the worker is stopping") },
                )

                shouldThrow<CancellationException> { realm.retireExpiredPendingAccounts() }
            }
        }

        "a stuck SSO claim" - {

            suspend fun stuckSsoClaim(real: AuthSystem.Deps) = real.storage.authRecords.create(
                AuthRecord.PendingActivation(
                    realm = realmId, ownerId = owner, claimedBy = AuthRecord.PendingActivation.CLAIMED_BY_SSO,
                )
            )

            fun realmOn(deps: AuthSystem.Deps, config: RealmTokenConfig, renamed: MutableList<EmailAddress>) =
                MinimalTestRealm(
                    getDeps = { deps },
                    tokenConfig = config,
                    onLoadUserById = { user },
                    onLoadUserByEmail = { if (it == email && renamed.isEmpty()) user else null },
                    onRetire = { u, retirement -> renamed += retirement.rename(u.value().email); u },
                )

            "is NOT retired before the account expires — it is still its SSO sign-in's to finish" {
                val real = realDeps()
                stuckSsoClaim(real)
                val renamed = mutableListOf<EmailAddress>()

                realmOn(real, RealmTokenConfig(), renamed).retireExpiredPendingAccounts() shouldBe 0

                renamed shouldBe emptyList()
                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner)
                    .shouldNotBeNull().value().claimedBy shouldBe AuthRecord.PendingActivation.CLAIMED_BY_SSO
            }

            "is TAKEN OVER and retired by the sweep once the account has expired" {
                val real = realDeps()
                stuckSsoClaim(real)
                val renamed = mutableListOf<EmailAddress>()

                realmOn(real, RealmTokenConfig(pendingAccountLifetime = Duration.ZERO), renamed)
                    .retireExpiredPendingAccounts() shouldBe 1

                renamed.size shouldBe 1
                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner).shouldBeNull()
            }

            "an SSO sign-in finishing its claim while the sweep takes it over loses cleanly: no session, retired" {
                val real = realDeps()
                stuckSsoClaim(real)
                real.storage.authRecords.create(AuthRecord.Password(realm = realmId, ownerId = owner, token = "pw"))
                val renamed = mutableListOf<EmailAddress>()
                val sweeper = realmOn(real, RealmTokenConfig(pendingAccountLifetime = Duration.ZERO), renamed)

                // The takeover lands right before SSO's final, conditional marker delete.
                val racing = depsWhereRemoveIfClaimedByRunsFirst(real) { sweeper.retireExpiredPendingAccounts() }
                val sso = realmOn(racing, RealmTokenConfig(pendingAccountLifetime = Duration.ZERO), renamed)

                shouldThrow<AuthError> { sso.resolveSsoAccount(user, provider = "test") { true } }
                    .message shouldBe "Invalid credentials"

                renamed.size shouldBe 1
                real.storage.authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realmId, owner).shouldBeNull()
            }
        }

        "refreshToken refuses a RETIRED account" {
            val retired = Stored(
                _id = user._id,
                value = MinimalTestUser(email = AccountRetirement.of(owner).rename(email)),
            )

            val realm = MinimalTestRealm(getDeps = { NullStorageTestDeps() }, onLoadUserById = { retired })

            shouldThrow<AuthError> {
                realm.refreshToken(owner, expectedUserType = null, currentOrgId = null)
            }
        }
    }

    /** [deps] whose marker lookups answer [stale] — what a request read before the stored marker changed. */
    private fun depsWhereMarkerReadIs(deps: AuthSystem.Deps, stale: Stored<AuthRecord.PendingActivation>): AuthSystem.Deps {
        val real = deps.storage.authRecords

        val adapter = object : AuthRecordStorage.Adapter by real.adapter {
            override suspend fun findLatest(realm: RealmId, type: String, owner: UserId): Stored<AuthRecord>? = when (type) {
                AuthRecord.PendingActivation.identifier -> stale
                else -> real.adapter.findLatest(realm, type, owner)
            }
        }

        val storage = AuthSystem.Storage(lazy { AuthRecordStorage(lazy { adapter }, lazy { real.kronos }) })

        return object : AuthSystem.Deps by deps {
            override val storage: AuthSystem.Storage = storage
        }
    }

    /** [deps] whose conditional removes run [first] (once) before they act. */
    private fun depsWhereRemoveIfClaimedByRunsFirst(deps: AuthSystem.Deps, first: suspend () -> Unit): AuthSystem.Deps {
        val real = deps.storage.authRecords
        var armed = true

        val adapter = object : AuthRecordStorage.Adapter by real.adapter {
            override suspend fun removeRecordIfClaimedBy(id: String, expected: String?): RemoveResult {
                if (armed) {
                    armed = false
                    first()
                }
                return real.adapter.removeRecordIfClaimedBy(id, expected)
            }
        }

        val storage = AuthSystem.Storage(lazy { AuthRecordStorage(lazy { adapter }, lazy { real.kronos }) })

        return object : AuthSystem.Deps by deps {
            override val storage: AuthSystem.Storage = storage
        }
    }

    /** [deps] whose `replaceRecordIfClaimedBy` is replaced by [onReplace]. */
    private fun depsWhereReplaceIfUnclaimed(deps: AuthSystem.Deps, onReplace: suspend () -> Stored<AuthRecord>?): AuthSystem.Deps {
        val real = deps.storage.authRecords

        val adapter = object : AuthRecordStorage.Adapter by real.adapter {
            @Suppress("UNCHECKED_CAST")
            override suspend fun <T : AuthRecord> replaceRecordIfClaimedBy(stored: Stored<T>, expected: String?): Stored<T>? =
                onReplace() as Stored<T>?
        }

        val storage = AuthSystem.Storage(lazy { AuthRecordStorage(lazy { adapter }, lazy { real.kronos }) })

        return object : AuthSystem.Deps by deps {
            override val storage: AuthSystem.Storage = storage
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
