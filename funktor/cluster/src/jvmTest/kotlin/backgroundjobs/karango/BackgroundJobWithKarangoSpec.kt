package de.peekandpoke.funktor.cluster.backgroundjobs.karango

import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsSpecBase
import de.peekandpoke.funktor.cluster.funktorCluster
import de.peekandpoke.funktor.core.broker.funktorBroker
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.funktorCore
import de.peekandpoke.funktor.core.model.AppInfo
import de.peekandpoke.funktor.core.model.default
import de.peekandpoke.funktor.rest.funktorRest
import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.karango.karango
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.security.user.UserProvider
import de.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

class BackgroundJobWithKarangoSpec : BackgroundJobsSpecBase() {

    override fun createKontainer(): Kontainer = runBlocking {
        kontainer {
            funktorCore(AppConfig.empty, AppInfo.default())
            funktorBroker()
            funktorRest()
            funktorCluster {
                useKarango()
            }
            karango(config = ArangoDbConfig.forUnitTests)
        }.create {
            with { UserProvider.anonymous }
        }.also {
            // Create the database repos
            it.use(Database::class) { ensureRepositories() }
        }
    }

    override fun prepareTest() {
        // Clear the database repos before each test
        runBlocking {
            kontainer.use(KarangoBackgroundJobsQueueRepo::class) { removeAll() }
            kontainer.use(KarangoBackgroundJobsArchiveRepo::class) { removeAll() }
        }
    }
}
