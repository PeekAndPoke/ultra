package io.peekandpoke.funktor.cluster.locks.monko

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
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking
import org.bson.Document
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("EXPERIMENTAL_API_USAGE")
class MonkoGlobalLockProviderSpec : VaultGlobalLocksProviderSpecBase() {

    private val kontainer: Kontainer by lazy {
        runBlocking {
            val config = AppConfig.empty

            kontainer {
                funktorCore(config, AppInfo.default())
                funktorBroker()
                funktorRest(config)
                funktorCluster { useMonko() }
                monko(config = MongoDbConfig.forUnitTests)
            }.create {
                with { UserProvider.anonymous }
            }.also {
                it.use(Database::class) { ensureRepositories() }
            }
        }
    }

    override val repo: MonkoGlobalLocksRepo get() = kontainer.get(MonkoGlobalLocksRepo::class)

    private val driver: MonkoDriver get() = kontainer.get(MonkoDriver::class)

    /** Writes a lock the way a version without heartbeats did: no token, no lastHeartbeat. */
    private suspend fun insertLegacy(key: String, created: MpInstant, expires: MpInstant) {
        @Suppress("UNCHECKED_CAST")
        val written = driver.codec.slumber(
            GlobalLockEntry(key = key, serverId = "old-server", created = created, expires = expires)
        ) as Map<String, Any?>

        driver.insertOne(repo.name, Document(written - "token" - "lastHeartbeat").append("_id", key))
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
