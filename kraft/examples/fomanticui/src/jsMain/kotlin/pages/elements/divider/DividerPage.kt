@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.divider

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.examples.fomanticui.helpers.shortParagraphWireFrame
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.fixture.LoremIpsum
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.DividerPage() = comp {
    DividerPage(it)
}

class DividerPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Divider")

        ui.basic.segment {
            ui.header H1 { +"Divider" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/divider.html#/definition")

            ui.dividing.header H2 { +"Types" }

            renderStandard()
            renderVertical()
            renderHorizontal()
            renderHorizontalAlignment()

            ui.dividing.header H2 { +"Variations" }

            renderInverted()
            renderFitted()
            renderHidden()
            renderSection()
            renderClearing()
        }
    }

    private fun FlowContent.renderStandard() = example {
        ui.header H2 { +"Standard Divider" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderStandard
        ) {
            // <CodeBlock renderStandard>
            ui.segment {

                ui.divider {}
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVertical() = example {
        ui.header H2 { +"Vertical Divider" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderVertical,
        ) {
            // <CodeBlock renderVertical>
            ui.segment {
                ui.two.column.very.relaxed.grid {
                    noui.column {
                        shortParagraphWireFrame()
                        shortParagraphWireFrame()
                    }
                    noui.column {
                        shortParagraphWireFrame()
                        shortParagraphWireFrame()
                    }
                }
                ui.vertical.divider { +"and" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHorizontal() = example {
        ui.header H2 { +"Horizontal Divider" }

        p { +"A divider can segment content horizontally" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderHorizontal_1,
        ) {
            // <CodeBlock renderHorizontal_1>
            shortParagraphWireFrame()

            ui.horizontal.divider { +"or" }

            shortParagraphWireFrame()
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderHorizontal_2,
        ) {
            // <CodeBlock renderHorizontal_2>
            ui.horizontal.divider.header {
                icon.tag()
                +"Description"
            }

            shortParagraphWireFrame()

            ui.horizontal.divider.header {
                icon.chart_bar()
                +"Specification"
            }

            shortParagraphWireFrame()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHorizontalAlignment() = example {
        ui.header H2 { +"Horizontal Alignment" }

        p { +"A horizontal divider can be aligned" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderHorizontalAlignment_1,
        ) {
            // <CodeBlock renderHorizontalAlignment_1>
            ui.horizontal.left.aligned.divider {
                icon.align_left()
                +"Left aligned"
            }
            ui.horizontal.center.aligned.divider {
                icon.align_center()
                +"Center aligned"
            }
            ui.horizontal.right.aligned.divider {
                icon.align_center()
                +"Right aligned"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.header H2 { +"Inverted" }

        p { +"A divider can have its colors inverted" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderInverted_1,
        ) {
            // <CodeBlock renderInverted_1>
            ui.inverted.segment {
                shortParagraphWireFrame()

                ui.inverted.divider { }

                shortParagraphWireFrame()

                ui.inverted.horizontal.divider { +"or" }

                shortParagraphWireFrame()
            }

            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFitted() = example {
        ui.header H2 { +"Fitted" }

        p { +"A divider can be fitted, without any space above or below it." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderFitted,
        ) {
            // <CodeBlock renderFitted>
            +LoremIpsum(words = 50)

            ui.fitted.divider { }

            +LoremIpsum(words = 50)
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHidden() = example {
        ui.header H2 { +"Hidden" }

        p { +"A hidden divider divides content without creating a dividing line" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderHidden,
        ) {
            // <CodeBlock renderHidden>
            +LoremIpsum(words = 50)

            ui.hidden.divider { }

            +LoremIpsum(words = 50)
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSection() = example {
        ui.header H2 { +"Section" }

        p { +"A divider can provide greater margins to divide sections of content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderSection,
        ) {
            // <CodeBlock renderSection>
            +LoremIpsum(words = 50)

            ui.section.divider { }

            +LoremIpsum(words = 50)
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderClearing() = example {
        ui.header H2 { +"Clearing" }

        p { +"A divider can clear the contents above it" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_divider_DividerPage_kt_renderClearing,
        ) {
            // <CodeBlock renderClearing>
            ui.segment {
                ui.right.floated.header { +"Right floated" }
                ui.clearing.divider {}
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }
}
