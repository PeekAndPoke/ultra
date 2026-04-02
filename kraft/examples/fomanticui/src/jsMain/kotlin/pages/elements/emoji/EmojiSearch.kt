package io.peekandpoke.kraft.examples.fomanticui.pages.elements.emoji

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.SemanticEmoji
import io.peekandpoke.ultra.semanticui.emoji
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.EmojiSearch() = comp {
    EmojiSearch(it)
}

class EmojiSearch(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var search by value("")

    private val filtered
        get(): List<String> {

            val parts = search.split(" ").filter { it.isNotBlank() }.map { it.lowercase() }

            return when (parts.isEmpty()) {
                true -> SemanticEmoji.all
                else -> SemanticEmoji.all.filter { name ->
                    parts.any { name.lowercase().contains(it) }
                }
            }
        }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.segment {
            ui.form {
                UiInputField(::search) {
                    placeholder("Search for emojis")
                }
            }
        }

        ui.segment {

            ui.eight.column.grid {
                filtered.forEach { name ->

                    ui.center.aligned.column {
                        emoji(name)
                        div {
                            +name
                        }
                    }
                }
            }
        }
    }
}
