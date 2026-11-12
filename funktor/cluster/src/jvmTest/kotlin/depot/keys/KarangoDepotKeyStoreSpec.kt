package io.peekandpoke.funktor.cluster.depot.keys

import io.peekandpoke.funktor.cluster.funktorCluster
import io.peekandpoke.funktor.core.broker.funktorBroker
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.funktorCore
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.core.model.default
import io.peekandpoke.funktor.rest.funktorRest
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.depot.crypto.DepotKeyStore
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.coroutines.runBlocking

class KarangoDepotKeyStoreSpec : DepotKeyStoreSpecBase() {

    override val kontainer: Kontainer by lazy {
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

    override fun profiledStore(profiler: QueryProfiler): DepotKeyStore = VaultDepotKeyStore(
        repo = KarangoDepotKeyRepo(driver = kontainer.get(KarangoDriver::class).withProfiler(profiler), repoName = "system_depot_keys"),
        kronos = Kronos.systemUtc,
    )
}
