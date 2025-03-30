package de.peekandpoke.kraft.examples.fomanticui.pages.elements.icon

import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.SemanticIcon
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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
