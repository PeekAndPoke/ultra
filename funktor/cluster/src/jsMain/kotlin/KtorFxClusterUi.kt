package de.peekandpoke.ktorfx.cluster

import de.peekandpoke.kraft.addons.routing.Router
import de.peekandpoke.kraft.addons.routing.RouterProvider
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.RenderFn
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobsArchivedListPage
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobsArchivedViewPage
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobsQueuedListPage
import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobsQueuedViewPage
import de.peekandpoke.ktorfx.cluster.depot.DepotBrowsePage
import de.peekandpoke.ktorfx.cluster.depot.DepotRepositoriesListPage
import de.peekandpoke.ktorfx.cluster.locks.GlobalLocksListPage
import de.peekandpoke.ktorfx.cluster.locks.ServerBeaconsListPage
import de.peekandpoke.ktorfx.cluster.storage.RandomCacheStorageListPage
import de.peekandpoke.ktorfx.cluster.storage.RandomCacheStorageViewPage
import de.peekandpoke.ktorfx.cluster.storage.RandomDataStorageListPage
import de.peekandpoke.ktorfx.cluster.storage.RandomDataStorageViewPage
import de.peekandpoke.ktorfx.cluster.vault.VaultIndexPage
import de.peekandpoke.ktorfx.cluster.workers.WorkerDetailsPage
import de.peekandpoke.ktorfx.cluster.workers.WorkersListPage
import de.peekandpoke.ultra.common.datetime.Kronos
import kotlinx.html.Tag
import org.w3c.dom.events.MouseEvent

class KtorFxClusterUi(
    val routerProvider: RouterProvider,
    val kronosProvider: () -> Kronos,
    val api: KtorFxClusterApiClient,
    val routes: KtorFxClusterRoutes,
    val customInternals: RenderFn = {},
) {
    /** Gets the router */
    val router: Router get() = routerProvider()

    /** Get the kronos */
    val kronos: Kronos get() = kronosProvider()

    //// Helpers /////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun navTo(route: KtorFxClusterRoutes.() -> String) {
        router.navToUri(
            routes.route()
        )
    }

    fun navTo(evt: MouseEvent, route: KtorFxClusterRoutes.() -> String) {
        router.navToUri(
            evt = evt,
            uri = routes.route()
        )
    }

    /**
     * Small helper to get [KtorFxClusterUi] as a this pointer into the scope
     */
    operator fun invoke(block: KtorFxClusterUi.() -> Unit) {
        this.block()
    }

    //// Overview ////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.KtorFxClusterOverviewPage() = comp(
        KtorFxClusterOverviewPage.Props(
            ui = this@KtorFxClusterUi,
            customInternals = customInternals,
        )
    ) {
        KtorFxClusterOverviewPage(it)
    }

    //// Locks Ui Components /////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.ServerBeaconsListPage() = comp(
        ServerBeaconsListPage.Props(ui = this@KtorFxClusterUi)
    ) {
        ServerBeaconsListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.GlobalLocksListPage() = comp(
        GlobalLocksListPage.Props(ui = this@KtorFxClusterUi)
    ) {
        GlobalLocksListPage(it)
    }

    //// BackgroundJobs Ui Components ////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsQueuedListPage() = comp(
        BackgroundJobsQueuedListPage.Props(ui = this@KtorFxClusterUi)
    ) {
        BackgroundJobsQueuedListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsQueuedViewPage(id: String) = comp(
        BackgroundJobsQueuedViewPage.Props(ui = this@KtorFxClusterUi, id = id)
    ) {
        BackgroundJobsQueuedViewPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsArchivedListPage() = comp(
        BackgroundJobsArchivedListPage.Props(ui = this@KtorFxClusterUi)
    ) {
        BackgroundJobsArchivedListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.BackgroundJobsArchivedViewPage(id: String) = comp(
        BackgroundJobsArchivedViewPage.Props(ui = this@KtorFxClusterUi, id = id)
    ) {
        BackgroundJobsArchivedViewPage(it)
    }

    //// Depot Ui Components /////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.DepotRepositoriesListPage() = comp(
        DepotRepositoriesListPage.Props(ui = this@KtorFxClusterUi)
    ) {
        DepotRepositoriesListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.DepotBrowsePage(repo: String, path: String) = comp(
        DepotBrowsePage.Props(ui = this@KtorFxClusterUi, repo = repo, path = path)
    ) {
        DepotBrowsePage(it)
    }

    //// Storage Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.RandomDataStorageListPage() = comp(
        RandomDataStorageListPage.Props(ui = this@KtorFxClusterUi)
    ) {
        RandomDataStorageListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomDataStorageViewPage(
        id: String,
    ) = comp(
        RandomDataStorageViewPage.Props(ui = this@KtorFxClusterUi, id = id)
    ) {
        RandomDataStorageViewPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomCacheStorageListPage() = comp(
        RandomCacheStorageListPage.Props(ui = this@KtorFxClusterUi)
    ) {
        RandomCacheStorageListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.RandomCacheStorageViewPage(
        id: String,
    ) = comp(
        RandomCacheStorageViewPage.Props(ui = this@KtorFxClusterUi, id = id)
    ) {
        RandomCacheStorageViewPage(it)
    }

    //// Vault Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.VaultIndexPage() = comp(
        VaultIndexPage.Props(ui = this@KtorFxClusterUi)
    ) {
        VaultIndexPage(it)
    }

    //// Workers Ui Components ///////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.WorkersListPage() = comp(
        WorkersListPage.Props(ui = this@KtorFxClusterUi)
    ) {
        WorkersListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.WorkerDetailsPage(id: String) = comp(
        WorkerDetailsPage.Props(ui = this@KtorFxClusterUi, id = id)
    ) {
        WorkerDetailsPage(it)
    }
}
