package de.peekandpoke.kraft.examples.fomanticui.pages.elements.flag

import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.SemanticFlag
import de.peekandpoke.kraft.semanticui.flag
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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
