package de.peekandpoke.kraft.examples.fomanticui.helpers

import de.peekandpoke.kraft.addons.prismjs.PrismKotlin
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.semantic
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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
