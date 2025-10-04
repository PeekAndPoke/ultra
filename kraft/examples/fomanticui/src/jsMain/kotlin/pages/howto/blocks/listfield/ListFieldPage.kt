package de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.listfield

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.forms.collections.ListField
import de.peekandpoke.kraft.routing.PageTitle
import de.peekandpoke.kraft.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.title

@Suppress("FunctionName")
fun Tag.ListFieldPage() = comp {
    ListFieldPage(it)
}

class ListFieldPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Data(
        val texts: List<String> = emptyList(),
    )

    private var data by value(Data()) {
        console.log(it.texts.toTypedArray())
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("How To | List Field")

        ui.basic.padded.segment {
            ui.dividing.header H1 { +"List Field" }

            renderSimpleExample()
        }
    }

    private fun FlowContent.renderSimpleExample() {
        ui.dividing.header H2 { +"Simple example" }

        ui.form {
            ui.segments {
                ListField(items = data.texts, onChange = { data = data.copy(texts = it) }) {
                    renderItem { ctx ->
                        ui.segment {
                            key = ctx.domKey()

                            UiTextArea(ctx.item, ctx.modifier { it }) {
                                +"Text ${ctx.idx + 1}"
                            }

                            ui.buttons {
                                upRemoveDownButtons(ctx)
                            }
                        }
                    }

                    renderAdd { ctx ->
                        ui.segment {
                            key = ctx.domKey()

                            ui.button {
                                onClick {
                                    ctx.add("")
                                }
                                icon.plus()
                                +"Add"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <T> FlowContent.upRemoveDownButtons(ctx: de.peekandpoke.kraft.forms.collections.ListFieldComponent.ItemCtx<T>) {

        if (ctx.idx > 0) {
            ui.basic.blue.icon.button {
                title = "Move Up"
                icon.arrow_up()
                onClick { ctx.swapWith(ctx.idx - 1) }
            }
        }

        ui.basic.red.icon.button {
            title = "Remove"
            icon.close()
            onClick { ctx.remove() }
        }

        if (ctx.idx < ctx.all.size - 1) {
            ui.basic.blue.icon.button {
                title = "Move Down"
                icon.arrow_down()
                onClick { ctx.swapWith(ctx.idx + 1) }
            }
        }
    }
}
