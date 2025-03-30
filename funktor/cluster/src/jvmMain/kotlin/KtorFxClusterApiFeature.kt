package de.peekandpoke.ktorfx.cluster

import de.peekandpoke.ktorfx.cluster.backgroundjobs.api.BackgroundJobsApi
import de.peekandpoke.ktorfx.cluster.depot.api.DepotApi
import de.peekandpoke.ktorfx.cluster.locks.api.GlobalLocksApi
import de.peekandpoke.ktorfx.cluster.storage.api.RandomCacheStorageApi
import de.peekandpoke.ktorfx.cluster.storage.api.RandomDataStorageApi
import de.peekandpoke.ktorfx.cluster.vault.api.VaultApi
import de.peekandpoke.ktorfx.cluster.workers.api.WorkersApi
import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiFeature

/**
 * Api feature for UpNext
 */
class KtorFxClusterApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "KtorFx Cluster"

    override val description = """
        Exposes information server internals.
    """.trimIndent()

    /** The Api endpoints for the global locks */
    val globalLocks = GlobalLocksApi(converter)

    /** The Api endpoints for the background jobs */
    val backgroundJobs = BackgroundJobsApi(converter)

    /** The Api endpoints for the vault database */
    val vault = VaultApi(converter)

    /** The Api endpoints for the workers */
    val workers = WorkersApi(converter)

    /** The Api endpoints for the depot */
    val depot = DepotApi(converter)

    /** The Api endpoints for the random data storage */
    val storageRandomData = RandomDataStorageApi(converter)
    val storageRandomCache = RandomCacheStorageApi(converter)

    override fun getRouteGroups() = listOf(
        globalLocks,
        backgroundJobs,
        vault,
        workers,
        depot,
        storageRandomData,
        storageRandomCache,
    )
}
