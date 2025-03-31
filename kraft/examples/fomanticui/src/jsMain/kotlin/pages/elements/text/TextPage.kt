@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.text

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.TextPage() = comp {
    TextPage(it)
}

class TextPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Text")

        ui.basic.segment {
            ui.dividing.header H1 { +"Text" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/text.html")

            ui.dividing.header H2 { +"Definition" }

            renderText()

            ui.dividing.header H2 { +"Variations" }

            renderSize()
        }
    }

    private fun FlowContent.renderText() = example {
        ui.dividing.header H3 { +"Styles" }

        p {
            +"To not interfere with all other colorable elements the "
            ui.label { +"text" }
            +" element is limited to be used in "
            ui.label { +"span" }
            +" tags only ."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_text_TextPage_kt_renderText_1,
        ) {
            // <CodeBlock renderText_1>
            ui.segment {
                +"This is "
                ui.red.text Span { +"red" }
                +" inline text and this is "
                ui.blue.text Span { +"blue" }
                +" inline text and this is "
                ui.purple.text Span { +"purple" }
                +" inline text"
            }

            ui.inverted.segment {
                +"This is "
                ui.red.text Span { +"red" }
                +" inline text and this is "
                ui.blue.text Span { +"blue" }
                +" inline text and this is "
                ui.purple.text Span { +"purple" }
                +" inline text"
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_text_TextPage_kt_renderText_2,
        ) {
            // <CodeBlock renderText_2>
            ui.segment {
                +"This is "
                ui.info.text Span { +"info" }
                +" inline text and this is "
                ui.success.text Span { +"success" }
                +" inline text and this is "
                ui.warning.text Span { +"warning" }
                +" inline text and this is "
                ui.error.text Span { +"error" }
                +" inline text"
            }

            ui.inverted.segment {
                +"This is "
                ui.info.text Span { +"info" }
                +" inline text and this is "
                ui.success.text Span { +"success" }
                +" inline text and this is "
                ui.warning.text Span { +"warning" }
                +" inline text and this is "
                ui.error.text Span { +"error" }
                +" inline text"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.dividing.header H3 { +"Styles" }

        ui.info.message {
            +"To not interfere with all other colorable elements the "
            ui.label { +"text" }
            +" element is limited to be used in "
            ui.label { +"span" }
            +" tags only ."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_text_TextPage_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            p { +"This is "; ui.mini.red.text Span { +"mini" }; +" text" }
            p { +"This is "; ui.tiny.red.text Span { +"tiny" }; +" text" }
            p { +"This is "; ui.small.red.text Span { +"small" }; +" text" }
            p { +"This is "; ui.medium.red.text Span { +"medium" }; +" text" }
            p { +"This is "; ui.large.red.text Span { +"large" }; +" text" }
            p { +"This is "; ui.big.red.text Span { +"big" }; +" text" }
            p { +"This is "; ui.huge.red.text Span { +"huge" }; +" text" }
            p { +"This is "; ui.massive.red.text Span { +"massive" }; +" text" }
            // </CodeBlock>
        }
    }
}
