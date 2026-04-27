package io.peekandpoke.funktor.cluster.locks.monko

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.cluster.funktorCluster
import io.peekandpoke.funktor.cluster.locks.GlobalServerId
import io.peekandpoke.funktor.cluster.locks.VaultGlobalLocksProvider
import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.core.broker.funktorBroker
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.funktorCore
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.core.model.default
import io.peekandpoke.funktor.rest.funktorRest
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("EXPERIMENTAL_API_USAGE")
class MonkoGlobalLockProviderSpec : StringSpec() {

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

    private val repo: MonkoGlobalLocksRepo get() = kontainer.get(MonkoGlobalLocksRepo::class)

    private fun createSubject() = VaultGlobalLocksProvider(
        repository = repo,
        serverId = GlobalServerId.withoutConfig(),
        retryDelayMs = 10,
    )

    init {
        beforeEach {
            repo.removeAll()
        }

        "An acquire on a key whose existing lock has expired succeeds without waiting for cleanup" {
            val key = "stale-key"
            val now = MpInstant.now()

            repo.insert(
                key = key,
                new = GlobalLockEntry(
                    key = key,
                    serverId = "dead-server",
                    created = now.minus(30.minutes),
                    expires = now.minus(15.minutes),
                ),
            )

            val subject = createSubject()
            val start = Kronos.systemUtc.instantNow()

            val result = subject.acquire(key, 2.seconds) { "got it" }.single()

            val elapsed = Kronos.systemUtc.instantNow() - start

            result shouldBe "got it"
            elapsed shouldBeLessThan 500.milliseconds
        }
    }
}
