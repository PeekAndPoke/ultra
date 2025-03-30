package de.peekandpoke.ktorfx.cluster.backgroundjobs.karango

import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.karango.karango
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobsSpecBase
import de.peekandpoke.ktorfx.cluster.ktorFxCluster
import de.peekandpoke.ktorfx.core.broker.ktorFxBroker
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.core.ktorFxCore
import de.peekandpoke.ktorfx.core.model.AppInfo
import de.peekandpoke.ktorfx.core.model.default
import de.peekandpoke.ktorfx.rest.ktorFxRest
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.security.user.UserProvider
import de.peekandpoke.ultra.vault.Database
import kotlinx.coroutines.runBlocking

class BackgroundJobWithKarangoSpec : BackgroundJobsSpecBase() {

    override fun createKontainer(): Kontainer = runBlocking {
        kontainer {
            ktorFxCore(AppConfig.empty, AppInfo.default())
            ktorFxBroker()
            ktorFxRest()
            ktorFxCluster {
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
