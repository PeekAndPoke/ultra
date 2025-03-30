package de.peekandpoke.ktorfx.logging

import de.peekandpoke.kraft.addons.modal.ModalsManager
import de.peekandpoke.kraft.addons.routing.RouterProvider
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.ktorfx.logging.api.LoggingApiClient
import kotlinx.html.Tag

class LoggingUi(
    val router: RouterProvider,
    val api: LoggingApiClient,
    val routes: LoggingRoutes,
    val modals: ModalsManager,
) {
    //// Helpers //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Small helper to get [LoggingUi] as a this pointer into the scope
     */
    operator fun invoke(block: LoggingUi.() -> Unit) {
        this.block()
    }

    //// Ui Components ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.LogEntriesListPage() = comp(
        LogEntriesListPage.Props(ui = this@LoggingUi)
    ) {
        LogEntriesListPage(it)
    }

    @Suppress("FunctionName")
    fun Tag.LogEntryDetailPage(id: String) =
        comp(LogEntryDetailPage.Props(ui = this@LoggingUi, id)) { LogEntryDetailPage(it) }
}
