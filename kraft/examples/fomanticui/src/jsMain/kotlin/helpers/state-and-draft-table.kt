package de.peekandpoke.kraft.examples.fomanticui.helpers

import de.peekandpoke.ultra.html.key
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.TD
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlin.reflect.KProperty1

data class PropRenderer<T, V>(
    val prop: KProperty1<T, V>,
    val toStr: (V) -> String?,
) {
    fun render(obj: T): String? {
        return prop.get(obj)?.let { toStr(it) }
    }
}

operator fun <T, V> KProperty1<T, V>.invoke(toStr: (V) -> String?) = PropRenderer(this, toStr)

fun <T> FlowContent.renderStateAndDraftTable(
    state: T,
    draft: T,
    properties: List<PropRenderer<T, *>>,
) {
    ui.striped.celled.table Table {
        thead {
            tr {
                th { +"Field" }
                th { +"State" }
                th { +"Draft" }
            }
        }
        tbody {

            properties.forEach { prop ->
                tr {
                    key = prop.prop.name

                    td("collapsing") { +prop.prop.name }
                    td {
                        renderCell(state, prop)
                    }
                    td {
                        renderCell(draft, prop)
                    }
                }
            }
        }
    }
}

private fun <T> TD.renderCell(obj: T, prop: PropRenderer<T, *>) {
    val value = prop.prop.get(obj)

    if (value != null) {
        ui.horizontal.list {
            noui.item {
                ui.basic.label { +"${value::class.simpleName}" }
            }
            noui.item {
                +"${prop.render(obj)}"
            }
        }
    } else {
        ui.basic.label { +"null" }
    }
}
