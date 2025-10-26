package de.peekandpoke.funktor.cluster

import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsArchivedListPage
import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsArchivedViewPage
import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsQueuedListPage
import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsQueuedViewPage
import de.peekandpoke.funktor.cluster.depot.DepotBrowsePage
import de.peekandpoke.funktor.cluster.depot.DepotRepositoriesListPage
import de.peekandpoke.funktor.cluster.devtools.DevtoolsRequestHistoryPage
import de.peekandpoke.funktor.cluster.locks.GlobalLocksListPage
import de.peekandpoke.funktor.cluster.locks.ServerBeaconsListPage
import de.peekandpoke.funktor.cluster.storage.RandomCacheStorageListPage
import de.peekandpoke.funktor.cluster.storage.RandomCacheStorageViewPage
import de.peekandpoke.funktor.cluster.storage.RandomDataStorageListPage
import de.peekandpoke.funktor.cluster.storage.RandomDataStorageViewPage
import de.peekandpoke.funktor.cluster.vault.VaultIndexPage
import de.peekandpoke.funktor.cluster.workers.WorkerDetailsPage
import de.peekandpoke.funktor.cluster.workers.WorkersListPage
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.html.RenderFn
import kotlinx.html.Tag

class FunktorClusterUi(
    kronos: () -> Kronos = { Kronos.systemUtc },
    val api: FunktorClusterApiClient,
    val routes: FunktorClusterRoutes = FunktorClusterRoutes(),
    val customInternals: RenderFn = {},
) {
    private val kronosProvider = kronos

    /** Get the kronos */
    val kronos: Kronos get() = kronosProvider()

    //// Helpers ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Small helper to get [FunktorClusterUi] as this pointer into the scope
     */
    operator fun invoke(block: FunktorClusterUi.() -> Unit) {
        this.block()
    }

    //// Overview //////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.FunktorClusterOverviewPage() = comp(
        FunktorClusterOverviewPage.Props(
            ui = this@FunktorClusterUi,
            customInternals = customInternals,
        )
    ) {
        FunktorClusterOverviewPage(it)
    }

    //// Locks Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.ServerBeaconsListPage() = comp(
        ServerBeaconsListPage.Props(ui = this@FunktorClusterUi)
    ) {
        ServerBeaconsListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.GlobalLocksListPage() = comp(
        GlobalLocksListPage.Props(ui = this@FunktorClusterUi)
    ) {
        GlobalLocksListPage(it)
    }

    //// BackgroundJobs Ui Components //////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsQueuedListPage() = comp(
        BackgroundJobsQueuedListPage.Props(ui = this@FunktorClusterUi)
    ) {
        BackgroundJobsQueuedListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsQueuedViewPage(id: String) = comp(
        BackgroundJobsQueuedViewPage.Props(ui = this@FunktorClusterUi, id = id)
    ) {
        BackgroundJobsQueuedViewPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsArchivedListPage() = comp(
        BackgroundJobsArchivedListPage.Props(ui = this@FunktorClusterUi)
    ) {
        BackgroundJobsArchivedListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsArchivedViewPage(id: String) = comp(
        BackgroundJobsArchivedViewPage.Props(ui = this@FunktorClusterUi, id = id)
    ) {
        BackgroundJobsArchivedViewPage(it)
    }

    //// Depot Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.DepotRepositoriesListPage() = comp(
        DepotRepositoriesListPage.Props(ui = this@FunktorClusterUi)
    ) {
        DepotRepositoriesListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.DepotBrowsePage(repo: String, path: String) = comp(
        DepotBrowsePage.Props(ui = this@FunktorClusterUi, repo = repo, path = path)
    ) {
        DepotBrowsePage(it)
    }

    //// Storage Ui Components /////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.RandomDataStorageListPage() = comp(
        RandomDataStorageListPage.Props(ui = this@FunktorClusterUi)
    ) {
        RandomDataStorageListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomDataStorageViewPage(
        id: String,
    ) = comp(
        RandomDataStorageViewPage.Props(ui = this@FunktorClusterUi, id = id)
    ) {
        RandomDataStorageViewPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomCacheStorageListPage() = comp(
        RandomCacheStorageListPage.Props(ui = this@FunktorClusterUi)
    ) {
        RandomCacheStorageListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomCacheStorageViewPage(
        id: String,
    ) = comp(
        RandomCacheStorageViewPage.Props(ui = this@FunktorClusterUi, id = id)
    ) {
        RandomCacheStorageViewPage(it)
    }

    //// Vault Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.VaultIndexPage() = comp(
        VaultIndexPage.Props(ui = this@FunktorClusterUi)
    ) {
        VaultIndexPage(it)
    }

    //// Workers Ui Components /////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.WorkersListPage() = comp(
        WorkersListPage.Props(ui = this@FunktorClusterUi)
    ) {
        WorkersListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.WorkerDetailsPage(id: String) = comp(
        WorkerDetailsPage.Props(ui = this@FunktorClusterUi, id = id)
    ) {
        WorkerDetailsPage(it)
    }

    //// Devtools Ui Components ////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.DevtoolsRequestHistoryPage() = comp(
        DevtoolsRequestHistoryPage.Props(
            ui = this@FunktorClusterUi,
            // TODO ...
            baseUrl = "http://admin.funktor-demo.local:36587/_/insights/details",
        )
    ) {
        DevtoolsRequestHistoryPage(it)
    }
}
