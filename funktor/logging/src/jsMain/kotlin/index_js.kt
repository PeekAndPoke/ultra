package de.peekandpoke.funktor.logging

import de.peekandpoke.funktor.logging.api.LogEntryModel
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.logging.LogLevel
import kotlinx.html.FlowContent

fun MpInstant.formatAtSystemDefault() = atSystemDefaultZone().format("yyyy-MM-dd HH:mm:ss.SSS")

fun LogLevel.renderLabel(flow: FlowContent) {
    val level = this

    with(flow) {
        when (level) {
            LogLevel.ERROR -> ui.red.label { +level.name }
            LogLevel.WARNING -> ui.orange.label { +level.name }
            LogLevel.INFO -> ui.blue.label { +level.name }
            else -> ui.label { +level.name }
        }
    }
}

fun LogEntryModel.State?.renderLabel(flow: FlowContent) {
    val state = this

    with(flow) {
        when (state) {
            null -> ui.grey.label { +"n/a" }
            LogEntryModel.State.New -> ui.yellow.label { +state.name }
            LogEntryModel.State.Ack -> ui.green.label { +state.name }
        }
    }
}
