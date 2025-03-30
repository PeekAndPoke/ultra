package de.peekandpoke.ktorfx.cluster.backgroundjobs

import de.peekandpoke.kraft.addons.routing.Route1
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.ktorfx.cluster.KtorFxClusterUi

class BackgroundJobsRoutes(mount: String) {

    val listQueued = Static("$mount/queued")
    val viewQueued = Route1("$mount/queued/{id}")
    fun viewQueued(id: String) = viewQueued.build(id)

    val listArchived = Static("$mount/archived")
    val viewArchived = Route1("$mount/archived/{id}")
    fun viewArchived(id: String) = viewArchived.build(id)
}

internal fun RouterBuilder.mountKtorFxBackgroundJobs(
    ui: KtorFxClusterUi,
) {
    mount(ui.routes.backgroundJobs.listQueued) { ui { BackgroundJobsQueuedListPage() } }
    mount(ui.routes.backgroundJobs.viewQueued) { ui { BackgroundJobsQueuedViewPage(it["id"]) } }

    mount(ui.routes.backgroundJobs.listArchived) { ui { BackgroundJobsArchivedListPage() } }
    mount(ui.routes.backgroundJobs.viewArchived) { ui { BackgroundJobsArchivedViewPage(it["id"]) } }
}
