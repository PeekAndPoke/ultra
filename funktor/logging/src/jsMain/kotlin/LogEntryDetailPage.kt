package de.peekandpoke.ktorfx.logging

import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ktorfx.logging.api.LogEntryModel
import de.peekandpoke.ktorfx.logging.api.LogsRequest
import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.css.Color
import kotlinx.css.Overflow
import kotlinx.css.Padding
import kotlinx.css.WhiteSpace
import kotlinx.css.backgroundColor
import kotlinx.css.overflow
import kotlinx.css.padding
import kotlinx.css.px
import kotlinx.css.whiteSpace
import kotlinx.html.FlowContent
import kotlinx.html.pre

class LogEntryDetailPage(ctx: Ctx<Props>) : Component<LogEntryDetailPage.Props>(ctx) {
    data class Props(
        val ui: LoggingUi,
        val id: String,
    )

    private val loader = dataLoader {
        val (ui, id) = props
        ui.api.get(id)
            .map { it.data!! }
    }

    private val noDblClick = doubleClickProtection()

    private suspend fun execAction(action: LogsRequest.Action) = noDblClick.runBlocking {
        props.ui.api.execAction(props.id, action).firstOrNull()

        loader.reloadSilently()
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("KtorFx", "Logs", props.id) }

        ui.padded.segment {
            loader.renderDefault(this) {
                header()
                content(it)
            }
        }
    }

    private fun FlowContent.header() {
        ui.dividing.header H2 { +"Log Entry '${props.id}'" }
    }

    private fun FlowContent.content(entry: LogEntryModel) {
        ui.cards {
            ui.card {
                noui.content {
                    noui.header { +"Log Level" }
                }
                noui.content {
                    entry.level.renderLabel(this)
                    ui.label { +entry.severity.toString() }
                }
            }

            ui.card {
                noui.content {
                    noui.header { +"Timestamp" }
                }
                noui.content {
                    val date = MpInstant.fromEpochMillis(entry.createdAt)

                    +date.formatAtSystemDefault()
                }
            }

            ui.card {
                noui.content {
                    noui.header { +"Server" }
                }
                noui.content {
                    +(entry.server ?: "n/a")
                }
            }

            ui.card {
                noui.content {
                    noui.header { +"State" }
                }

                noui.content {
                    entry.state.renderLabel(this)
                }

                noui.content {
                    when (entry.state) {
                        LogEntryModel.State.New -> ui.card {
                            ui.green.given(noDblClick.cannotRun) { loading }.button {
                                onClick {
                                    launch {
                                        execAction(LogsRequest.Action.SetState(LogEntryModel.State.Ack))
                                    }
                                }
                                +"Acknowledge"
                            }
                        }

                        LogEntryModel.State.Ack -> ui.card {
                            ui.yellow.given(noDblClick.cannotRun) { loading }.button {
                                onClick {
                                    launch {
                                        execAction(LogsRequest.Action.SetState(LogEntryModel.State.New))
                                    }
                                }
                                +"Un-Acknowledge"
                            }
                        }
                    }
                }
            }
        }

        ui.dividing.header { +"Logger" }
        pre { +(entry.loggerName ?: "n/a") }

        preformattedTextSection(
            headerTitle = "Message",
            content = entry.message
        )

        entry.stackTrace?.let { trace ->
            preformattedTextSection(
                headerTitle = "Stack Trace",
                content = trace,
            )
        }
    }

    private fun FlowContent.preformattedTextSection(headerTitle: String, content: String?) {
        ui.dividing.header {
            +headerTitle
        }

        pre {
            css {
                whiteSpace = WhiteSpace.pre
                padding = Padding(5.px)
                backgroundColor = Color("#F0F0F0")
                overflow = Overflow.auto
            }
            +(content ?: "N/A")
        }
    }
}
