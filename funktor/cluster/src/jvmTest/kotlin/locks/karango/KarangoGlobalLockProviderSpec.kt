package io.peekandpoke.funktor.cluster.locks.karango

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.cluster.funktorCluster
import io.peekandpoke.funktor.cluster.locks.LocksException
import io.peekandpoke.funktor.cluster.locks.VaultGlobalLocksProviderSpecBase
import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.core.broker.funktorBroker
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.funktorCore
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.core.model.default
import io.peekandpoke.funktor.rest.funktorRest
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("EXPERIMENTAL_API_USAGE")
class KarangoGlobalLockProviderSpec : VaultGlobalLocksProviderSpecBase() {

    private val kontainer: Kontainer by lazy {
        runBlocking {
            val config = AppConfig.empty

            kontainer {
                funktorCore(config, AppInfo.default())
                funktorBroker()
                funktorRest(config)
                funktorCluster { useKarango() }
                karango(config = ArangoDbConfig.forUnitTests)
            }.create {
                with { UserProvider.anonymous }
            }.also {
                it.use(Database::class) { ensureRepositories() }
            }
        }
    }

    override val repo: KarangoGlobalLocksRepo get() = kontainer.get(KarangoGlobalLocksRepo::class)

    private val driver: KarangoDriver get() = kontainer.get(KarangoDriver::class)

    // Karango runs every query on Dispatchers.IO itself (karango/core/src/main/kotlin/vault/KarangoDriver.kt:120),
    // so a saturated IO pool stalls the beat's query, whatever dispatcher the beat runs on.
    override val beatsThroughSaturatedIo: Boolean get() = false

    /** Writes a lock the way a version without heartbeats did: no token, no lastHeartbeat. */
    private suspend fun insertLegacy(key: String, created: MpInstant, expires: MpInstant) {
        @Suppress("UNCHECKED_CAST")
        val written = driver.codec.slumber(
            GlobalLockEntry(key = key, serverId = "old-server", created = created, expires = expires)
        ) as Map<String, Any?>

        driver.arangoDb.collection(repo.name)
            .insertDocument(written - "token" - "lastHeartbeat" + ("_key" to key))
            .await()
    }

    init {
        "A record written before heartbeats existed decodes, and is taken over only once it has expired" {
            val now = MpInstant.now()
            insertLegacy("alive", created = now.minus(1.hours), expires = now.plus(10.minutes))
            insertLegacy("expired", created = now.minus(30.minutes), expires = now.minus(15.minutes))

            stored("alive")!!.let {
                it.token shouldBe ""
                it.lastHeartbeat.toEpochMillis() shouldBe it.created.toEpochMillis()
            }

            val subject = createSubject(ttl = 300.milliseconds)

            shouldThrow<LocksException.Timeout> {
                subject.lock("alive", timeout = 300.milliseconds) { "stolen" }
            }

            subject.lock("expired", timeout = 2.seconds) { "taken over" } shouldBe "taken over"
        }

        "removeIfUnchanged removes a record written before heartbeats existed" {
            insertLegacy("legacy", created = MpInstant.now().minus(1.hours), expires = MpInstant.now().minus(1.minutes))

            repo.removeIfUnchanged(stored("legacy")!!) shouldBe true
            stored("legacy") shouldBe null
        }
    }
}
