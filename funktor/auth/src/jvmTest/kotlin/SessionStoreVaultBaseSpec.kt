package io.peekandpoke.funktor.auth

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import kotlin.time.Duration.Companion.days

abstract class SessionStoreVaultBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()

    abstract fun FunktorAuthBuilder.configureAuth()

    suspend fun createKontainer(): Kontainer {
        return createAuthTestContainer(
            configureKontainer = { configureKontainer() },
            configureAuth = { configureAuth() },
        )
    }

    private suspend fun Kontainer.newStore(): SessionStore.Vault {
        val storage = funktorAuth.deps.storage.authRecords
        storage.adapter.removeAll()
        return SessionStore.Vault(
            storage = storage,
            random = AuthRandom.default,
            kronos = get(Kronos::class),
        )
    }

    init {
        "create + getById round-trips a session" {
            val store = createKontainer().newStore()

            val created = store.create(
                realm = "r",
                ownerId = "u1",
                deviceFingerprint = "fp1",
                userAgent = "ua",
                ipAddress = "127.0.0.1",
                ttl = 30.days,
            )

            val token = created.resolve().token

            val found = store.getById("r", token)
            found.shouldNotBeNull()
            found.resolve().ownerId shouldBe "u1"
            found.resolve().deviceFingerprint shouldBe "fp1"
        }

        "getById respects realm scoping" {
            val store = createKontainer().newStore()

            val created = store.create(
                realm = "r1", ownerId = "u1", deviceFingerprint = "fp",
                userAgent = null, ipAddress = null, ttl = 30.days,
            )
            val token = created.resolve().token

            store.getById("r1", token).shouldNotBeNull()
            store.getById("r2", token).shouldBeNull()
        }

        "listForUser returns every session owned by the user" {
            val store = createKontainer().newStore()

            store.create(
                realm = "r", ownerId = "u1", deviceFingerprint = "a",
                userAgent = null, ipAddress = null, ttl = 30.days,
            )
            store.create(
                realm = "r", ownerId = "u1", deviceFingerprint = "b",
                userAgent = null, ipAddress = null, ttl = 30.days,
            )
            store.create(
                realm = "r", ownerId = "u2", deviceFingerprint = "c",
                userAgent = null, ipAddress = null, ttl = 30.days,
            )

            val u1Sessions = store.listForUser("r", "u1")
            u1Sessions shouldHaveSize 2

            val u2Sessions = store.listForUser("r", "u2")
            u2Sessions shouldHaveSize 1
        }

        "revoke removes the session so subsequent getById returns null" {
            val store = createKontainer().newStore()

            val created = store.create(
                realm = "r", ownerId = "u1", deviceFingerprint = "fp",
                userAgent = null, ipAddress = null, ttl = 30.days,
            )
            val token = created.resolve().token

            store.getById("r", token).shouldNotBeNull()

            store.revoke("r", token)

            store.getById("r", token).shouldBeNull()
        }

        "revokeAllForUser removes every session for the user" {
            val store = createKontainer().newStore()

            store.create("r", "u1", "a", null, null, 30.days)
            store.create("r", "u1", "b", null, null, 30.days)
            store.create("r", "u2", "c", null, null, 30.days)

            store.revokeAllForUser("r", "u1")

            store.listForUser("r", "u1") shouldHaveSize 0

            withClue("Other users' sessions survive") {
                store.listForUser("r", "u2") shouldHaveSize 1
            }
        }

        "revokeAllForUser with except keeps the excepted session alive" {
            val store = createKontainer().newStore()

            val keep = store.create("r", "u1", "keep", null, null, 30.days)
            store.create("r", "u1", "drop1", null, null, 30.days)
            store.create("r", "u1", "drop2", null, null, 30.days)

            val keepToken = keep.resolve().token

            store.revokeAllForUser("r", "u1", except = keepToken)

            val remaining = store.listForUser("r", "u1")
            remaining shouldHaveSize 1
            remaining.first().resolve().token shouldBe keepToken
        }

        "computeDeviceFingerprint is deterministic for the same inputs" {
            val a = SessionStore.computeDeviceFingerprint(userAgent = "Mozilla/5.0", ipAddress = "10.0.0.1")
            val b = SessionStore.computeDeviceFingerprint(userAgent = "Mozilla/5.0", ipAddress = "10.0.0.1")
            val c = SessionStore.computeDeviceFingerprint(userAgent = "Chrome", ipAddress = "10.0.0.1")

            a shouldBe b
            (a != c) shouldBe true
        }
    }
}
