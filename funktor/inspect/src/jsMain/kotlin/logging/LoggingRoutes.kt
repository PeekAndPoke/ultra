package io.peekandpoke.funktor.inspect.logging

import io.peekandpoke.funktor.inspect.logging.api.LogEntryModel
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

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
