package io.peekandpoke.kraft.examples.fomanticui.helpers

import io.peekandpoke.kraft.addons.prismjs.PrismKotlin
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import io.peekandpoke.kraft.addons.styling.RawStyleSheet
import io.peekandpoke.kraft.addons.styling.StyleSheet
import io.peekandpoke.kraft.addons.styling.StyleSheets
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.semantic
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.css.Overflow
import kotlinx.css.overflow
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div

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

private object CodePanelStyles : StyleSheet() {
    val container by rule {
        put("display", "flex")
        put("flex-direction", "column")
        put("flex", "1")
        overflow = Overflow.auto
    }

    // Descendant rules need raw CSS — kotlinx-css nested blocks don't produce descendant selectors.
    private val descendants = RawStyleSheet(
        autoMount = false,
        css = """
            .${container.name} > div { flex: 1; display: flex; flex-direction: column; }
            .${container.name} .code-toolbar { flex: 1; display: flex; flex-direction: column; }
            .${container.name} pre[class*="language"] { flex: 1 1 0%; min-height: 0; margin: 0; }
        """.trimIndent(),
    )

    init {
        StyleSheets.mount(descendants)
    }
}

/**
 * Renders a SemanticUI column containing a PrismKotlin code block that fills the available row height.
 *
 * This emits a `ui.column` with flex styling, so call it directly inside a grid — not inside another column.
 */
@Suppress("FunctionName")
fun FlowContent.CodePanelColumn(code: String) {
    ui.column {
        attributes["style"] = "display: flex; flex-direction: column;"

        div(CodePanelStyles.container.name) {
            PrismKotlin(code.trimIndent()) {
                lineNumbers()
                copyToClipboard()
            }
        }
    }
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
