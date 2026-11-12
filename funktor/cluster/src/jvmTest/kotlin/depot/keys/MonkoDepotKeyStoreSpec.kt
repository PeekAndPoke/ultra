package io.peekandpoke.funktor.cluster.depot.keys

import io.peekandpoke.funktor.cluster.funktorCluster
import io.peekandpoke.funktor.core.broker.funktorBroker
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.funktorCore
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.core.model.default
import io.peekandpoke.funktor.rest.funktorRest
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

class MonkoDepotKeyStoreSpec : DepotKeyStoreSpecBase() {

    override val kontainer: Kontainer by lazy {
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
}
