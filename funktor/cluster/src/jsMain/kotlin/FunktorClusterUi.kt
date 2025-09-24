package de.peekandpoke.funktor.cluster

import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsArchivedListPage
import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsArchivedViewPage
import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsQueuedListPage
import de.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobsQueuedViewPage
import de.peekandpoke.funktor.cluster.depot.DepotBrowsePage
import de.peekandpoke.funktor.cluster.depot.DepotRepositoriesListPage
import de.peekandpoke.funktor.cluster.locks.GlobalLocksListPage
import de.peekandpoke.funktor.cluster.locks.ServerBeaconsListPage
import de.peekandpoke.funktor.cluster.storage.RandomCacheStorageListPage
import de.peekandpoke.funktor.cluster.storage.RandomCacheStorageViewPage
import de.peekandpoke.funktor.cluster.storage.RandomDataStorageListPage
import de.peekandpoke.funktor.cluster.storage.RandomDataStorageViewPage
import de.peekandpoke.funktor.cluster.vault.VaultIndexPage
import de.peekandpoke.funktor.cluster.workers.WorkerDetailsPage
import de.peekandpoke.funktor.cluster.workers.WorkersListPage
import de.peekandpoke.kraft.addons.routing.Router
import de.peekandpoke.kraft.addons.routing.RouterProvider
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.semanticui.RenderFn
import kotlinx.html.Tag
import org.w3c.dom.events.MouseEvent

class FunktorClusterUi(
    val routerProvider: RouterProvider,
    val kronosProvider: () -> Kronos,
    val api: FunktorClusterApiClient,
    val routes: FunktorClusterRoutes,
    val customInternals: RenderFn = {},
) {
    /** Gets the router */
    val router: Router get() = routerProvider()

    /** Get the kronos */
    val kronos: Kronos get() = kronosProvider()

    //// Helpers /////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun navTo(route: FunktorClusterRoutes.() -> String) {
        router.navToUri(
            routes.route()
        )
    }

    fun navTo(evt: MouseEvent, route: FunktorClusterRoutes.() -> String) {
        router.navToUri(
            evt = evt,
            uri = routes.route()
        )
    }

    /**
     * Small helper to get [FunktorClusterUi] as this pointer into the scope
     */
    operator fun invoke(block: FunktorClusterUi.() -> Unit) {
        this.block()
    }

    //// Overview ////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.FunktorClusterOverviewPage() = comp(
        FunktorClusterOverviewPage.Props(
            ui = this@FunktorClusterUi,
            customInternals = customInternals,
        )
    ) {
        FunktorClusterOverviewPage(it)
    }

    //// Locks Ui Components /////////////////////////////////////////////////////////////////////////////////////////////

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

    //// BackgroundJobs Ui Components ////////////////////////////////////////////////////////////////////////////////////

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

    //// Depot Ui Components /////////////////////////////////////////////////////////////////////////////////////////////

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

    //// Storage Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

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

    //// Workers Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

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
}
