package io.peekandpoke.kraft.examples.jsaddons.helpers

import io.peekandpoke.kraft.addons.prismjs.PrismKotlin
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.HorizontalContentAndCode(
    code: String,
    example: FlowContent.() -> Unit,
) = comp(
    ContentAndCode.Props(
        code = code,
        example = example,
    )
) {
    ContentAndCode(it)
}

class ContentAndCode(ctx: Ctx<Props>) : Component<ContentAndCode.Props>(ctx) {

    data class Props(
        val code: String,
        val example: FlowContent.() -> Unit,
    )

    override fun VDom.render() {
        ui.stackable.two.column.grid {
            ui.column {
                props.example(this)
            }

            ui.column {
                PrismKotlin(props.code.trimIndent()) {
                    lineNumbers()
                    copyToClipboard()
                }
            }
        }
    }
}
