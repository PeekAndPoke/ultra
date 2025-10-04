package de.peekandpoke.funktor.logging

import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.kraft.routing.Route1
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

class LoggingRoutes(mount: String = "/_/funktor/logging") {

    val list = Static(mount)
    val view = Route1("$mount/{id}")
    fun view(id: String) = view.bind(id)
    fun view(entry: LogEntryModel) = view(id = entry.id)
}

fun RouterBuilder.mountFunktorLogging(
    ui: LoggingUi,
) {
    mount(ui.routes.list) { ui { LogEntriesListPage() } }
    mount(ui.routes.view) { ui { LogEntryDetailPage(it["id"]) } }
}
