package de.peekandpoke.ktorfx.logging

import de.peekandpoke.kraft.addons.routing.Route1
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.ktorfx.logging.api.LogEntryModel

class LoggingRoutes(mount: String = "/_/ktorfx/logging") {

    val list = Static(mount)
    val view = Route1("$mount/{id}")
    fun view(id: String) = view.build(id)
    fun view(entry: LogEntryModel) = view(id = entry.id)
}

fun RouterBuilder.mountKtorFxLogging(
    ui: LoggingUi,
) {
    mount(ui.routes.list) { ui { LogEntriesListPage() } }
    mount(ui.routes.view) { ui { LogEntryDetailPage(it["id"]) } }
}
