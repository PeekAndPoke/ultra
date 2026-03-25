package io.peekandpoke.kraft.examples.fomanticui.pages.elements.flag

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.SemanticFlag
import io.peekandpoke.ultra.semanticui.flag
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.FlagSearch() = comp {
    FlagSearch(it)
}

class FlagSearch(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var search by value("")

    private val filtered
        get(): List<String> {

            val parts = search.split(" ").filter { it.isNotBlank() }.map { it.lowercase() }

            return when (parts.isEmpty()) {
                true -> SemanticFlag.all
                else -> SemanticFlag.all.filter { name ->
                    parts.any { name.lowercase().contains(it) }
                }
            }
        }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.segment {
            ui.form {
                UiInputField(::search) {
                    placeholder("Search for flags")
                }
            }
        }

        ui.segment {

            ui.ten.column.grid {
                filtered.forEach { name ->
                    ui.center.aligned.column {
                        flag.with(name).render()
                        div {
                            +name
                        }
                    }
                }
            }
        }
    }
}
