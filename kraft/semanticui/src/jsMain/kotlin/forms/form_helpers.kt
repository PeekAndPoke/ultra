package io.peekandpoke.kraft.semanticui.forms

import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent

fun <P> io.peekandpoke.kraft.forms.FormField<P>.renderErrors(flow: FlowContent) {
    renderErrors(this, flow)
}

fun <P> renderErrors(field: io.peekandpoke.kraft.forms.FormField<P>, flow: FlowContent) {
    flow.apply {
        if (field.touched) {
            field.errors.filter { it.isNotBlank() }.forEach { error ->
                ui.basic.red.pointing.label { +error }
            }
        }
    }
}
