package io.peekandpoke.kraft.examples.fomanticui.helpers

import io.peekandpoke.kraft.addons.prismjs.PrismKotlin
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.semantic
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.HorizontalContentAndCode(
    code: String,
    example: FlowContent.() -> Unit,
) = ContentAndCode(
    code = code,
    direction = ContentAndCode.Direction.Horizontal,
    example = example,
)

@Suppress("FunctionName")
fun Tag.VerticalContentAndCode(
    code: String,
    example: FlowContent.() -> Unit,
) = ContentAndCode(
    code = code,
    direction = ContentAndCode.Direction.Vertical,
    example = example,
)

@Suppress("FunctionName")
fun Tag.ContentAndCode(
    code: String,
    direction: ContentAndCode.Direction,
    example: FlowContent.() -> Unit,
) = comp(
    ContentAndCode.Props(
        code = code,
        direction = direction,
        example = example,
    )
) {
    ContentAndCode(it)
}

class ContentAndCode(ctx: Ctx<Props>) : Component<ContentAndCode.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val code: String,
        val direction: Direction,
        val example: FlowContent.() -> Unit,
    )

    enum class Direction {
        Horizontal,
        Vertical,
    }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        val format = when (props.direction) {
            Direction.Horizontal -> semantic { two }
            Direction.Vertical -> semantic { one }
        }

        ui.stackable.format().column.grid {
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
