package io.peekandpoke.funktor.auth.provider

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.FunktorAuthBuilder
import io.peekandpoke.funktor.auth.MinimalTestRealm
import io.peekandpoke.funktor.auth.MinimalTestUser
import io.peekandpoke.funktor.auth.createAuthTestContainer
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.funktorAuth
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Stored

class SignInRaceKarangoSpec : SignInRaceBaseSpec() {
    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useKarango()
    }
}

class SignInRaceMonkoSpec : SignInRaceBaseSpec() {
    override fun KontainerBuilder.configureKontainer() {
        monko(MongoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useMonko()
    }
}

/**
 * A password sign-in IN FLIGHT across a one-way flow, against real storage: it proved the password, then a
 * verified SSO activation (or a reset completion) changed that password and removed the marker, and only then
 * does the sign-in read the marker. It must not leave with a session on the now-active account.
 */
abstract class SignInRaceBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()

    abstract fun FunktorAuthBuilder.configureAuth()

    private val realmId = RealmId("test-realm")
    private val email = EmailAddress.of("victim@example.com")
    private val user = Stored(_id = "test_users/victim", value = MinimalTestUser(email = email))
    private val owner = UserId(user._id)
    private val attackerPassword = "Attacker-1234!"

    private suspend fun realDeps(): AuthSystem.Deps = createAuthTestContainer(
        configureKontainer = { configureKontainer() },
        configureAuth = { configureAuth() },
    ).funktorAuth.deps.also { it.storage.authRecords.adapter.removeAll() }

    /** [deps] whose FIRST marker lookup runs [between] first — the window after the password check. */
    private fun depsWithGateBeforeMarkerRead(deps: AuthSystem.Deps, between: suspend () -> Unit): AuthSystem.Deps {
        val real = deps.storage.authRecords
        var armed = true

        val adapter = object : AuthRecordStorage.Adapter by real.adapter {
            override suspend fun findLatest(realm: RealmId, type: String, owner: UserId): Stored<AuthRecord>? {
                if (armed && type == AuthRecord.PendingActivation.identifier) {
                    armed = false
                    between()
                }
                return real.adapter.findLatest(realm, type, owner)
            }
        }

        val storage = AuthSystem.Storage(lazy { AuthRecordStorage(lazy { adapter }, lazy { real.kronos }) })

        return object : AuthSystem.Deps by deps {
            override val storage: AuthSystem.Storage = storage
        }
    }

    private fun providerOn(deps: AuthSystem.Deps) = EmailAndPasswordAuth(
        frontendUrls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c"),
        log = NullLog,
        services = lazy { EmailAndPasswordAuth.Services(deps) },
    )

    private val request = AuthSignInRequest.EmailAndPassword(
        provider = EmailAndPasswordAuth.ID, email = email.value, password = attackerPassword,
    )

    private suspend fun AuthSystem.Deps.preRegister(pending: Boolean) {
        storage.authRecords.create(
            AuthRecord.Password(realm = realmId, ownerId = owner, token = passwordHasher.hashAsString(attackerPassword))
        )
        if (pending) storage.authRecords.create(AuthRecord.PendingActivation(realm = realmId, ownerId = owner))
    }

    init {
        "a sign-in that proved the password just before a verified SSO activation gets NO session" {
            val real = realDeps()
            real.preRegister(pending = true)
            val ssoRealm = MinimalTestRealm(getDeps = { real })

            // Between the password check and the marker read, the victim activates with Google: password gone,
            // marker gone.
            val raced = depsWithGateBeforeMarkerRead(real) {
                ssoRealm.resolveSsoAccount(user, provider = "google-sso") { true }
            }

            shouldThrow<AuthError> {
                providerOn(raced).signIn(MinimalTestRealm(getDeps = { raced }, onLoadUserByEmail = { user }), request)
            }.message shouldBe "Invalid credentials"
        }

        "a sign-in that proved the OLD password just before a reset completion gets NO session" {
            val real = realDeps()
            real.preRegister(pending = true)

            // The reset completion's effect, in its order: a NEW password record, then the marker goes.
            val raced = depsWithGateBeforeMarkerRead(real) {
                real.storage.authRecords.create(
                    AuthRecord.Password(realm = realmId, ownerId = owner, token = real.passwordHasher.hashAsString("New-1234!"))
                )
                real.storage.authRecords.removeAllByOwner(AuthRecord.PendingActivation, realmId, owner)
            }

            shouldThrow<AuthError> {
                providerOn(raced).signIn(MinimalTestRealm(getDeps = { raced }, onLoadUserByEmail = { user }), request)
            }.message shouldBe "Invalid credentials"
        }

        "a plain sign-in of an active account still works" {
            val real = realDeps()
            real.preRegister(pending = false)

            providerOn(real).signIn(MinimalTestRealm(getDeps = { real }, onLoadUserByEmail = { user }), request) shouldBe user
        }
    }
}
