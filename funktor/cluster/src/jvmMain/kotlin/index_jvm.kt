package de.peekandpoke.ktorfx.cluster

import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobs
import de.peekandpoke.ktorfx.cluster.backgroundjobs.example.ExampleBackgroundJobHandler01
import de.peekandpoke.ktorfx.cluster.backgroundjobs.example.ExampleBackgroundJobHandler02
import de.peekandpoke.ktorfx.cluster.backgroundjobs.karango.KarangoBackgroundJobsArchiveRepo
import de.peekandpoke.ktorfx.cluster.backgroundjobs.karango.KarangoBackgroundJobsQueueRepo
import de.peekandpoke.ktorfx.cluster.backgroundjobs.workers.BackgroundJobsWorker
import de.peekandpoke.ktorfx.cluster.depot.DepotFacade
import de.peekandpoke.ktorfx.cluster.locks.GlobalLocksProvider
import de.peekandpoke.ktorfx.cluster.locks.GlobalServerId
import de.peekandpoke.ktorfx.cluster.locks.GlobalServerList
import de.peekandpoke.ktorfx.cluster.locks.LocksFacade
import de.peekandpoke.ktorfx.cluster.locks.PrimitiveGlobalLocksProvider
import de.peekandpoke.ktorfx.cluster.locks.ServerBeaconRepository
import de.peekandpoke.ktorfx.cluster.locks.VaultGlobalLocksProvider
import de.peekandpoke.ktorfx.cluster.locks.cli.ReleaseLocksCliCommand
import de.peekandpoke.ktorfx.cluster.locks.karango.KarangoGlobalLocksRepo
import de.peekandpoke.ktorfx.cluster.locks.karango.KarangoServerBeaconRepo
import de.peekandpoke.ktorfx.cluster.locks.lifecycle.GlobalLocksCleanupOnAppStarting
import de.peekandpoke.ktorfx.cluster.locks.lifecycle.GlobalLocksCleanupOnAppStopped
import de.peekandpoke.ktorfx.cluster.locks.workers.GlobalLocksCleanupWorker
import de.peekandpoke.ktorfx.cluster.locks.workers.ServerBeaconCleanupWorker
import de.peekandpoke.ktorfx.cluster.locks.workers.ServerBeaconUpdateWorker
import de.peekandpoke.ktorfx.cluster.storage.RandomCacheStorage
import de.peekandpoke.ktorfx.cluster.storage.RandomDataStorage
import de.peekandpoke.ktorfx.cluster.storage.StorageFacade
import de.peekandpoke.ktorfx.cluster.storage.example.RandomDataStorage_Example01
import de.peekandpoke.ktorfx.cluster.storage.fixtures.RandomCacheStorageFixtures
import de.peekandpoke.ktorfx.cluster.storage.fixtures.RandomDataStorageFixtures
import de.peekandpoke.ktorfx.cluster.storage.karango.KarangoRandomCacheRepository
import de.peekandpoke.ktorfx.cluster.storage.karango.KarangoRandomDataRepository
import de.peekandpoke.ktorfx.cluster.vault.EnsureRepositoriesOnAppStarting
import de.peekandpoke.ktorfx.cluster.workers.WorkersFacade
import de.peekandpoke.ktorfx.cluster.workers.fixtures.WorkerFixtures
import de.peekandpoke.ktorfx.cluster.workers.services.WorkerHistory
import de.peekandpoke.ktorfx.cluster.workers.services.WorkerRegistry
import de.peekandpoke.ktorfx.cluster.workers.services.WorkerTracker
import de.peekandpoke.ktorfx.cluster.workers.vault.KarangoWorkerHistoryRepo
import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun KontainerBuilder.ktorFxCluster(
    builder: KtorFXClusterBuilder.() -> Unit = {},
) = module(KtorFX_Cluster, builder)

inline val KontainerAware.cluster: KtorFxClusterFacade get() = kontainer.get()
inline val ApplicationCall.cluster: KtorFxClusterFacade get() = kontainer.cluster
inline val RoutingContext.cluster: KtorFxClusterFacade get() = call.cluster

inline val KontainerAware.database: Database get() = kontainer.get(Database::class)
inline val ApplicationCall.database: Database get() = kontainer.database
inline val RoutingContext.database: Database get() = call.database

/**
 * Background workers kontainer module
 */
val KtorFX_Cluster = module { builder: KtorFXClusterBuilder.() -> Unit ->

    // The Facade for all essential services ////////////////////////////////////////
    singleton(KtorFxClusterFacade::class)
    // Api facade
    singleton(KtorFxClusterApiFeature::class)

    // //////////////////////////////////////////////////////////////////////////////
    // BackgroundJobs
    // //
    singleton(BackgroundJobs::class)
    singleton(BackgroundJobs.DataCodec::class)
    singleton(BackgroundJobs.Queue::class) { BackgroundJobs.Queue.Null() }
    singleton(BackgroundJobs.Archive::class) { BackgroundJobs.Archive.Null() }
    dynamic(BackgroundJobsWorker::class)
    // Examples
    dynamic(ExampleBackgroundJobHandler01::class)
    dynamic(ExampleBackgroundJobHandler02::class)

    // //////////////////////////////////////////////////////////////////////////////
    // Depot
    // //
    singleton(DepotFacade::class)

    /////////////////////////////////////////////////////////////////////////////////
    // Locks
    // //
    singleton(LocksFacade::class)
    singleton(GlobalServerId::class)
    singleton(GlobalServerList::class)
    dynamic(GlobalLocksProvider::class) { PrimitiveGlobalLocksProvider() }
    dynamic(ServerBeaconRepository::class) { ServerBeaconRepository.Null }
    // Application lifecycle
    dynamic(GlobalLocksCleanupOnAppStarting::class)
    dynamic(GlobalLocksCleanupOnAppStopped::class)
    // workers
    dynamic(ServerBeaconUpdateWorker::class)
    dynamic(ServerBeaconCleanupWorker::class)
    dynamic(GlobalLocksCleanupWorker::class)
    // Cli
    dynamic(ReleaseLocksCliCommand::class)

    // //////////////////////////////////////////////////////////////////////////////
    // Storage
    // //
    singleton(StorageFacade::class)

    // Random data storage
    singleton(RandomDataStorage::class)
    singleton(RandomDataStorage.Adapter::class) { RandomDataStorage.Adapter.Null }
    // Fixtures
    singleton(RandomDataStorageFixtures::class)
    // Examples
    singleton(RandomDataStorage_Example01::class)

    // Random Cache
    singleton(RandomCacheStorage::class)
    singleton(RandomCacheStorage.Adapter::class) { RandomCacheStorage.Adapter.Null }
    // Fixtures
    singleton(RandomCacheStorageFixtures::class)

    // //////////////////////////////////////////////////////////////////////////////
    // Vault
    // //
    singleton(EnsureRepositoriesOnAppStarting::class)

    // //////////////////////////////////////////////////////////////////////////////
    // Workers
    // //
    dynamic(WorkersFacade::class)
    singleton(WorkerRegistry::class)
    instance(WorkerTracker)
    singleton(WorkerHistory::class)
    singleton(WorkerHistory.Adapter::class) { WorkerHistory.Adapter.InMemory }

    // //////////////////////////////////////////////////////////////////////////////
    // Apply external configuration
    // //
    KtorFXClusterBuilder(this).apply(builder)
}

class KtorFXClusterBuilder internal constructor(private val kontainer: KontainerBuilder) {

    fun useKarango(
        globalLockRepoName: String = "system_global_locks",
        serverBeaconRepoName: String = "system_server_beacons",
        workerHistoryRepoName: String = "system_worker_history",
        backgroundJobsQueueRepoName: String = "system_background_jobs_queue",
        backgroundJobsArchiveRepoName: String = "system_background_jobs_archive",
        storageRandomDataRepoName: String = "system_random_data",
        storageRandomCacheRepoName: String = "system_random_cache",
    ) {
        with(kontainer) {

            // ////////////////////////////////////////////////////////////////////////////////////////////////
            // Background Jobs
            // //

            // Job Queue //////////////////////////////////////////////////////////////////////////////////////
            singleton(KarangoBackgroundJobsQueueRepo::class) { driver: KarangoDriver ->
                KarangoBackgroundJobsQueueRepo(
                    driver = driver,
                    repoName = backgroundJobsQueueRepoName,
                )
            }

            singleton(KarangoBackgroundJobsQueueRepo.Fixtures::class)

            singleton(BackgroundJobs.Queue::class, BackgroundJobs.Queue.Vault::class)

            // Job Archive ////////////////////////////////////////////////////////////////////////////////////
            singleton(KarangoBackgroundJobsArchiveRepo::class) { driver: KarangoDriver ->
                KarangoBackgroundJobsArchiveRepo(
                    driver = driver,
                    repoName = backgroundJobsArchiveRepoName,
                )
            }

            singleton(KarangoBackgroundJobsArchiveRepo.Fixtures::class)

            singleton(BackgroundJobs.Archive::class, BackgroundJobs.Archive.Vault::class)

            // ////////////////////////////////////////////////////////////////////////////////////////////////
            // Global Locks
            // //

            singleton(KarangoGlobalLocksRepo::class) { driver: KarangoDriver ->
                KarangoGlobalLocksRepo(
                    driver = driver,
                    repoName = globalLockRepoName,
                )
            }

            singleton(KarangoGlobalLocksRepo.Fixtures::class)

            singleton(GlobalLocksProvider::class) { repository: KarangoGlobalLocksRepo, serverId: GlobalServerId ->
                VaultGlobalLocksProvider(
                    repository = repository,
                    serverId = serverId,
                    retryDelayMs = 100,
                )
            }

            singleton(KarangoServerBeaconRepo::class) { driver: KarangoDriver ->
                KarangoServerBeaconRepo(
                    driver = driver,
                    repoName = serverBeaconRepoName,
                )
            }

            singleton(KarangoServerBeaconRepo.Fixtures::class)

            singleton(ServerBeaconRepository::class, ServerBeaconRepository.Vault::class)

            // /////////////////////////////////////////////////////////////////////////////////////////////////
            // Storage
            // //

            // Random data /////////////////////////////////////////////////////////////////////////////////////
            singleton(RandomDataStorage.Adapter.Vault.Repo::class) { driver: KarangoDriver, timestamped: TimestampedHook ->
                KarangoRandomDataRepository(
                    driver = driver,
                    repoName = storageRandomDataRepoName,
                    timestamped = timestamped,
                )
            }

            singleton(RandomDataStorage.Adapter::class, RandomDataStorage.Adapter.Vault::class)

            // Random cache ////////////////////////////////////////////////////////////////////////////////////
            singleton(RandomCacheStorage.Adapter.Vault.Repo::class) { driver: KarangoDriver, timestamped: TimestampedHook ->
                KarangoRandomCacheRepository(
                    driver = driver,
                    repoName = storageRandomCacheRepoName,
                    timestamped = timestamped,
                )
            }

            singleton(RandomCacheStorage.Adapter::class, RandomCacheStorage.Adapter.Vault::class)

            // /////////////////////////////////////////////////////////////////////////////////////////////////
            // Workers
            // //

            singleton(WorkerHistory.Adapter.Vault.Repo::class) { driver: KarangoDriver ->
                KarangoWorkerHistoryRepo(driver = driver, repoName = workerHistoryRepoName)
            }

            singleton(WorkerFixtures::class)

            singleton(WorkerHistory.Adapter::class, WorkerHistory.Adapter.Vault::class)
        }
    }
}
