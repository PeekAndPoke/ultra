package de.peekandpoke.funktor.logging

import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.funktor.logging.api.LogsRequest
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.forms.formController
import de.peekandpoke.kraft.modals.ModalsManager
import de.peekandpoke.kraft.modals.ModalsManager.Companion.modals
import de.peekandpoke.kraft.semanticui.forms.UiDateTimeField
import de.peekandpoke.kraft.semanticui.modals.FadingModal
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.ultra.common.datetime.MpTimezone
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import de.peekandpoke.ultra.html.debugId
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag

fun <T> Component<T>.showLogsBulkActionPopup(ui: LoggingUi, onClose: (() -> Unit)? = null) {
    modals.show { handle ->
        handle.onClose { onClose?.invoke() }

        LogsBulkActionPopup(handle, ui)
    }
}

@Suppress("FunctionName")
fun Tag.LogsBulkActionPopup(
    handle: ModalsManager.Handle,
    ui: LoggingUi,
) = comp(
    LogsBulkActionPopup.Props(
        handle = handle,
        ui = ui,
    )
) {
    LogsBulkActionPopup(it)
}

class LogsBulkActionPopup(ctx: Ctx<Props>) : FadingModal<LogsBulkActionPopup.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        override val handle: ModalsManager.Handle,
        val ui: LoggingUi,
    ) : FadingModal.Props()

    private sealed class Result {
        data class Success(
            val result: LogsRequest.BulkResponse,
        ) : Result()

        data class Failed(
            val error: String,
        ) : Result()
    }

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val noDblClick = doubleClickProtection()
    private val formCtrl = formController()

    private var lastResult: Result? by value(null)

    private var from: MpZonedDateTime? by value(null)
    private var to: MpZonedDateTime? by value(null)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    private fun getFilter() = LogsRequest.BulkAction.Filter(from = from?.toInstant(), to = to?.toInstant())

    private suspend fun execAction(action: LogsRequest.BulkAction) = noDblClick.runBlocking {
        lastResult = null

        props.ui.api.execBulkAction(action)
            .catch { e ->
                console.error("Could not execute bulk action", e)
                lastResult = Result.Failed(e.stackTraceToString())
            }
            .map { Result.Success(it.data!!) }
            .firstOrNull()
            ?.let {
                lastResult = it
            }
    }

    override fun FlowContent.renderContent() {

        ui.small.modal.transition.visible.active.front {
            debugId("logs-bulk-actions")

            ui.header H3 {
                +"Logs Bulk Actions"
            }

            ui.scrolling.content {
                ui.form {

                    when (val r = lastResult) {
                        null -> {}
                        is Result.Success -> ui.blue.message { +"Last bulk action modified ${r.result.numChanged} entries" }
                        is Result.Failed -> ui.red.message { +"Last bulk action failed: ${r.error}" }
                    }

                    ui.two.fields {
                        UiDateTimeField.nullable(
                            value = from,
                            timezone = MpTimezone.systemDefault,
                            onChange = { from = it }
                        ) {
                            label("Starting from")
                        }

                        UiDateTimeField.nullable(
                            value = to,
                            timezone = MpTimezone.systemDefault,
                            onChange = { to = it }
                        ) {
                            label("Ending at")
                        }
                    }
                }
            }

            noui.actions {
                ui.basic.negative.button {
                    onClick {
                        close()
                    }
                    icon.times()
                    +"Cancel"
                }

                val canProceed = formCtrl.isValid

                ui.givenNot(canProceed) { disabled }.givenNot(noDblClick.canRun) { loading.disabled }.orange.button {
                    onClick {
                        if (formCtrl.validate()) {
                            launch {
                                execAction(
                                    LogsRequest.BulkAction(
                                        filter = getFilter().copy(
                                            states = LogEntryModel.State.except(LogEntryModel.State.New),
                                        ),
                                        action = LogsRequest.Action.SetState(state = LogEntryModel.State.New),
                                    ),
                                )
                            }
                        }
                    }
                    icon.square()
                    +"Nack messages"
                }

                ui.givenNot(canProceed) { disabled }.givenNot(noDblClick.canRun) { loading.disabled }.green.button {
                    onClick {
                        if (formCtrl.validate()) {
                            launch {
                                execAction(
                                    LogsRequest.BulkAction(
                                        filter = getFilter().copy(
                                            states = LogEntryModel.State.except(LogEntryModel.State.Ack),
                                        ),
                                        action = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                                    ),
                                )
                            }
                        }
                    }
                    icon.check_square()
                    +"Ack messages"
                }
            }
        }
    }
}
