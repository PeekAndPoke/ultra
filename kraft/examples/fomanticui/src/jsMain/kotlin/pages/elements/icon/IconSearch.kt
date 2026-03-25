package io.peekandpoke.kraft.examples.fomanticui.pages.elements.icon

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.SemanticIcon
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.IconSearch() = comp {
    IconSearch(it)
}

class IconSearch(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var search by value("")

    private val filtered
        get(): List<String> {

            val parts = search.split(" ").filter { it.isNotBlank() }.map { it.lowercase() }

            return when (parts.isEmpty()) {
                true -> SemanticIcon.all
                else -> SemanticIcon.all.filter { name ->
                    parts.any { name.lowercase().contains(it) }
                }
            }
        }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.segment {
            ui.form {
                UiInputField(::search) {
                    placeholder("Search for icons")
                }
            }
        }

        ui.segment {

            ui.ten.column.grid {
                filtered.forEach { name ->

                    ui.center.aligned.column {
                        icon.big.with(name).render()
                        div {
                            +name
                        }
                    }
                }
            }
        }
    }
}
