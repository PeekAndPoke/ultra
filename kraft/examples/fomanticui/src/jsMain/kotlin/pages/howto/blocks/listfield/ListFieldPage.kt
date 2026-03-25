package io.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.listfield

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.collections.ListField
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.semanticui.forms.UiTextArea
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
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

    private fun <T> FlowContent.upRemoveDownButtons(ctx: io.peekandpoke.kraft.forms.collections.ListFieldComponent.ItemCtx<T>) {

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
