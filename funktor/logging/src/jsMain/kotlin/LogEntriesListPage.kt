package de.peekandpoke.funktor.logging

import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.forms.old.select.SelectField
import de.peekandpoke.kraft.addons.semanticui.pagination.PaginationEpp
import de.peekandpoke.kraft.addons.semanticui.pagination.PaginationPages
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.routing.JoinedPageTitle
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.toggle
import de.peekandpoke.ultra.logging.LogLevel
import de.peekandpoke.ultra.semanticui.css
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.css.WhiteSpace
import kotlinx.css.whiteSpace
import kotlinx.html.FlowContent
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.label
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.unsafe

class LogEntriesListPage(ctx: Ctx<Props>) : Component<LogEntriesListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: LoggingUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    val loader = dataLoader {
        props.ui.api.list(filter).map { it.data!! }
    }

    var filter: LogsFilter by logsFilter(props.ui.router()) {
        loader.reloadSilently()
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Logs") }

        ui.padded.segment {
            header()

            loader.renderDefault(this) {
                ui.attached.segment { pagination(it) }

                list(it)

                ui.attached.segment { pagination(it) }
            }
        }
    }

    private fun FlowContent.header() {
        ui.header H2 { +"Server Logs" }

        ui.yellow.message {
            icon.exclamation()
            +"NOTICE: These logs might be incomplete. You will only see the logs that where recorded into the database."
        }

        ui.form {
            ui.fields {
                ui.three.wide.field {
                    SelectField(filter.minLevel, { filter = filter.copy(minLevel = it).firstPage() }) {
                        label("Log Level")
                        LogLevel.entries.filter { it != LogLevel.OFF }.forEach {
                            option(it, it.name)
                        }
                    }
                }

                ui.three.wide.field {
                    label { +"State" }

                    ui.horizontal.list {
                        LogEntryModel.State.entries.forEach { state ->
                            noui.item {
                                val selected = filter.state.contains(state)
                                ui.given(selected) { blue }.label {
                                    onClick { filter = filter.copy(state = filter.state.toggle(state)).firstPage() }
                                    +state.name
                                }
                            }
                        }
                    }
                }

                ui.ten.wide.field {
                    UiInputField(filter.search, { filter = filter.copy(search = it, page = 1) }) {
                        label("Search")
                        placeholder("Search Logs")
                        autofocus()
                        rightClearingIcon()
                    }
                }

                ui.two.wide.field {
                    label { unsafe { +"&nbsp;" } }
                    ui.primary.icon.button {
                        title = "Reload"
                        onClick { loader.reload() }
                        icon.redo()
                    }

                    ui.primary.icon.button {
                        title = "Bulk Actions"
                        onClick { showLogsBulkActionPopup(props.ui) }
                        icon.tools()
                    }
                }
            }
        }
    }

    private fun FlowContent.list(entries: Paged<LogEntryModel>) {
        ui.attached.striped.selectable.table Table {
            thead {
                tr {
                    th { +"Level" }
                    th { +"Timestamp" }
                    th { +"State" }
                    th { +"Server" }
                    th { +"Logger" }
                    th { +"Message" }
                }
            }
            tbody {
                entries.items.forEach { entry ->
                    tr {
                        onClick { event ->
                            props.ui.router().navToUri(
                                uri = props.ui.routes.view(entry),
                                evt = event
                            )
                        }

                        classes = when (entry.level) {
                            LogLevel.ERROR -> classes.plus("negative")
                            LogLevel.WARNING -> classes.plus("warning")
                            else -> classes
                        }

                        td { // Level
                            entry.level.renderLabel(this)
                        }
                        td { // TS
                            css { whiteSpace = WhiteSpace.nowrap }

                            val date = MpInstant.fromEpochMillis(entry.createdAt)

                            +date.formatAtSystemDefault()
                        }
                        td {
                            entry.state.renderLabel(this)
                        }
                        td {
                            +(entry.server ?: "n/a")
                        }
                        td { // Logger
                            entry.loggerName?.let {
                                title = it
                            }

                            +entry.shortenedLoggerName
                        }
                        td { // message
                            val lines = listOfNotNull(
                                entry.message,
                                entry.stackTrace
                            ).joinToString("\n").split("\n")

                            lines.take(3).forEach { div { +it } }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.pagination(entries: Paged<LogEntryModel>) {
        ui.horizontal.list {
            ui.item {
                PaginationPages(activePage = filter.page, totalPages = entries.fullPageCount?.toInt()) {
                    filter = filter.copy(page = it)
                }
            }
            ui.item {
                PaginationEpp(epp = filter.epp) {
                    filter = filter.copy(epp = it).firstPage()
                }
            }
        }
    }
}
