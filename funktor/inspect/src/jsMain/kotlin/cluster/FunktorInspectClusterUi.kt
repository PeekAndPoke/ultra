package io.peekandpoke.funktor.inspect.cluster

import io.peekandpoke.funktor.inspect.cluster.backgroundjobs.BackgroundJobsArchivedListPage
import io.peekandpoke.funktor.inspect.cluster.backgroundjobs.BackgroundJobsArchivedViewPage
import io.peekandpoke.funktor.inspect.cluster.backgroundjobs.BackgroundJobsQueuedListPage
import io.peekandpoke.funktor.inspect.cluster.backgroundjobs.BackgroundJobsQueuedViewPage
import io.peekandpoke.funktor.inspect.cluster.depot.DepotBrowsePage
import io.peekandpoke.funktor.inspect.cluster.depot.DepotRepositoriesListPage
import io.peekandpoke.funktor.inspect.cluster.devtools.DevtoolsRequestHistoryPage
import io.peekandpoke.funktor.inspect.cluster.locks.GlobalLocksListPage
import io.peekandpoke.funktor.inspect.cluster.locks.ServerBeaconsListPage
import io.peekandpoke.funktor.inspect.cluster.storage.RandomCacheStorageListPage
import io.peekandpoke.funktor.inspect.cluster.storage.RandomCacheStorageViewPage
import io.peekandpoke.funktor.inspect.cluster.storage.RandomDataStorageListPage
import io.peekandpoke.funktor.inspect.cluster.storage.RandomDataStorageViewPage
import io.peekandpoke.funktor.inspect.cluster.vault.VaultIndexPage
import io.peekandpoke.funktor.inspect.cluster.workers.WorkerDetailsPage
import io.peekandpoke.funktor.inspect.cluster.workers.WorkersListPage
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.html.RenderFn
import kotlinx.html.Tag

class FunktorInspectClusterUi(
    kronos: () -> Kronos = { Kronos.systemUtc },
    val api: FunktorClusterApiClient,
    val routes: FunktorClusterRoutes = FunktorClusterRoutes(),
    val customInternals: RenderFn = {},
) {
    private val kronosProvider = kronos

    /** Get the kronos */
    val kronos: Kronos get() = kronosProvider()

    // // Helpers ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Small helper to get [FunktorInspectClusterUi] as this pointer into the scope
     */
    operator fun invoke(block: FunktorInspectClusterUi.() -> Unit) {
        this.block()
    }

    // // Overview //////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.FunktorClusterOverviewPage() = comp(
        FunktorInspectClusterOverviewPage.Props(
            ui = this@FunktorInspectClusterUi,
            customInternals = customInternals,
        )
    ) {
        FunktorInspectClusterOverviewPage(it)
    }

    // // Locks Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.ServerBeaconsListPage() = comp(
        ServerBeaconsListPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        ServerBeaconsListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.GlobalLocksListPage() = comp(
        GlobalLocksListPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        GlobalLocksListPage(it)
    }

    // // BackgroundJobs Ui Components //////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsQueuedListPage() = comp(
        BackgroundJobsQueuedListPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        BackgroundJobsQueuedListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsQueuedViewPage(id: String) = comp(
        BackgroundJobsQueuedViewPage.Props(ui = this@FunktorInspectClusterUi, id = id)
    ) {
        BackgroundJobsQueuedViewPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsArchivedListPage() = comp(
        BackgroundJobsArchivedListPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        BackgroundJobsArchivedListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsArchivedViewPage(id: String) = comp(
        BackgroundJobsArchivedViewPage.Props(ui = this@FunktorInspectClusterUi, id = id)
    ) {
        BackgroundJobsArchivedViewPage(it)
    }

    // // Depot Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.DepotRepositoriesListPage() = comp(
        DepotRepositoriesListPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        DepotRepositoriesListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.DepotBrowsePage(repo: String, path: String) = comp(
        DepotBrowsePage.Props(ui = this@FunktorInspectClusterUi, repo = repo, path = path)
    ) {
        DepotBrowsePage(it)
    }

    // // Storage Ui Components /////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.RandomDataStorageListPage() = comp(
        RandomDataStorageListPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        RandomDataStorageListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomDataStorageViewPage(
        id: String,
    ) = comp(
        RandomDataStorageViewPage.Props(ui = this@FunktorInspectClusterUi, id = id)
    ) {
        RandomDataStorageViewPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomCacheStorageListPage() = comp(
        RandomCacheStorageListPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        RandomCacheStorageListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomCacheStorageViewPage(
        id: String,
    ) = comp(
        RandomCacheStorageViewPage.Props(ui = this@FunktorInspectClusterUi, id = id)
    ) {
        RandomCacheStorageViewPage(it)
    }

    // // Vault Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.VaultIndexPage() = comp(
        VaultIndexPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        VaultIndexPage(it)
    }

    // // Workers Ui Components /////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.WorkersListPage() = comp(
        WorkersListPage.Props(ui = this@FunktorInspectClusterUi)
    ) {
        WorkersListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.WorkerDetailsPage(id: String) = comp(
        WorkerDetailsPage.Props(ui = this@FunktorInspectClusterUi, id = id)
    ) {
        WorkerDetailsPage(it)
    }

    // // Devtools Ui Components ////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.DevtoolsRequestHistoryPage() = comp(
        DevtoolsRequestHistoryPage.Props(
            ui = this@FunktorInspectClusterUi,
        )
    ) {
        DevtoolsRequestHistoryPage(it)
    }
}
