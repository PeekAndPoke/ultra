package io.peekandpoke.funktor.logging

import io.peekandpoke.funktor.logging.api.LoggingApiClient
import io.peekandpoke.kraft.components.comp
import kotlinx.html.Tag

class LoggingUi(
    val api: LoggingApiClient,
    val routes: LoggingRoutes = LoggingRoutes(),
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
