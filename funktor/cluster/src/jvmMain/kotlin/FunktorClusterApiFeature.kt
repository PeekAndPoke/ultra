package de.peekandpoke.funktor.cluster

import de.peekandpoke.funktor.cluster.backgroundjobs.api.BackgroundJobsApi
import de.peekandpoke.funktor.cluster.depot.api.DepotApi
import de.peekandpoke.funktor.cluster.locks.api.GlobalLocksApi
import de.peekandpoke.funktor.cluster.storage.api.RandomCacheStorageApi
import de.peekandpoke.funktor.cluster.storage.api.RandomDataStorageApi
import de.peekandpoke.funktor.cluster.vault.api.VaultApi
import de.peekandpoke.funktor.cluster.workers.api.WorkersApi
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiFeature

/**
 * Api feature for UpNext
 */
class FunktorClusterApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "Funktor Cluster"

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
