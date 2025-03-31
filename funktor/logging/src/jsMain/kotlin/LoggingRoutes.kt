package de.peekandpoke.funktor.logging

import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.kraft.addons.routing.Route1
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static

class LoggingRoutes(mount: String = "/_/funktor/logging") {

    val list = Static(mount)
    val view = Route1("$mount/{id}")
    fun view(id: String) = view.build(id)
    fun view(entry: LogEntryModel) = view(id = entry.id)
}

fun RouterBuilder.mountFunktorLogging(
    ui: LoggingUi,
) {
    mount(ui.routes.list) { ui { LogEntriesListPage() } }
    mount(ui.routes.view) { ui { LogEntryDetailPage(it["id"]) } }
}
