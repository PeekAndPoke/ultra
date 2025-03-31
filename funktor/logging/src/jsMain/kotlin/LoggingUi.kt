package de.peekandpoke.funktor.logging

import de.peekandpoke.funktor.logging.api.LoggingApiClient
import de.peekandpoke.kraft.addons.modal.ModalsManager
import de.peekandpoke.kraft.addons.routing.RouterProvider
import de.peekandpoke.kraft.components.comp
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
