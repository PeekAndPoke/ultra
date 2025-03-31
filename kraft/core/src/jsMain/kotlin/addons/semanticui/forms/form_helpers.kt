package de.peekandpoke.kraft.addons.semanticui.forms

import de.peekandpoke.kraft.addons.forms.FormField
import de.peekandpoke.kraft.semanticui.ui
import kotlinx.html.FlowContent

fun <P> FormField<P>.renderErrors(flow: FlowContent) {
    renderErrors(this, flow)
}

fun <P> renderErrors(field: FormField<P>, flow: FlowContent) {
    flow.apply {
        if (field.touched) {
            field.errors.filter { it.isNotBlank() }.forEach { error ->
                ui.basic.red.pointing.label { +error }
            }
        }
    }
}
