@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.header

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.examples.fomanticui.helpers.*
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.css.fontSize
import kotlinx.css.px
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.p
import kotlinx.html.span

@Suppress("FunctionName")
fun Tag.HeaderPage() = comp {
    HeaderPage(it)
}

class HeaderPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var size by value(20)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Header")

        ui.basic.segment {
            ui.dividing.header H1 { +"Header" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/header.html")

            ui.dividing.header H2 { +"Types" }

            renderPageHeaders()
            renderCustomHeaders()
            renderIconHeaders()
            renderSubHeaders()

            ui.dividing.header H2 { +"Content" }

            renderImageHeaders()
            renderIconContentHeaders()
            renderSubHeaderContentHeaders()

            ui.dividing.header H2 { +"Variations" }

            renderDisabledHeaders()
            renderDividingHeaders()
            renderBlockHeaders()
            renderAttachedHeaders()
            renderFloatingHeaders()
            renderTextAlignmentHeaders()
            renderColoredHeaders()
            renderInvertedHeaders()
        }
    }

    private fun FlowContent.renderPageHeaders() = example {
        ui.header H3 { +"Page headers" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderPageHeaders,
        ) {
            // <CodeBlock renderPageHeaders>
            ui.header H1 { +"First header" }
            shortParagraphWireFrame()

            ui.header H2 { +"Seconds header" }
            shortParagraphWireFrame()

            ui.header H3 { +"Third header" }
            shortParagraphWireFrame()

            ui.header H4 { +"Fourth header" }
            shortParagraphWireFrame()

            ui.header H5 { +"Fifth header" }
            shortParagraphWireFrame()

            ui.header H6 { +"Sixth header" }
            shortParagraphWireFrame()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCustomHeaders() = example {
        ui.header H3 { +"Custom headers" }

        ui.buttons {
            ui.icon.button {
                onClick { size-- }
                icon.minus()
            }
            ui.icon.button {
                onClick { size++ }
                icon.plus()
            }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderCustomHeaders,
        ) {
            // <CodeBlock renderCustomHeaders>
            ui.vertical.segment {
                css {
                    fontSize = size.px
                }

                ui.massive.header { +"Massive header" }
                shortParagraphWireFrame()

                ui.huge.header { +"Huge header" }
                shortParagraphWireFrame()

                ui.big.header { +"Big header" }
                shortParagraphWireFrame()

                ui.large.header { +"Large header" }
                shortParagraphWireFrame()

                ui.medium.header { +"Medium header" }
                shortParagraphWireFrame()

                ui.small.header { +"Small header" }
                shortParagraphWireFrame()

                ui.tiny.header { +"Tiny header" }
                shortParagraphWireFrame()

                ui.mini.header { +"Mini header" }
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIconHeaders() = example {
        ui.header H3 { +"Icon headers" }

        p { +"A header can be formatted to emphasize an icon" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderIconHeaders_1,
        ) {
            // <CodeBlock renderIconHeaders_1>
            ui.center.aligned.icon.header H2 {
                icon.cogs()
                noui.content {
                    +"Account Settings"
                    noui.sub.header {
                        +"Manage your account settings and set e-mail preferences."
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderIconHeaders_2,
        ) {
            // <CodeBlock renderIconHeaders_2>
            ui.center.aligned.icon.header H2 {
                icon.circular.users()
                noui.content {
                    +"Friends"
                    shortParagraphWireFrame()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSubHeaders() = example {
        ui.header H3 { +"Sub headers" }

        p { +"Headers may be formatted to label smaller or de-emphasized content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderSubHeaders_1,
        ) {
            // <CodeBlock renderSubHeaders_1>
            ui.sub.header { +"Price" }
            span { +"€ 10,99" }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderSubHeaders_2,
        ) {
            // <CodeBlock renderSubHeaders_2>
            ui.horizontal.list {
                noui.item {
                    ui.mini.circular.image Img {
                        src = "/images/avatar2/large/molly.png"
                    }
                    noui.content {
                        ui.sub.header { +"Molly" }
                        +"Coordindator"
                    }
                }
                noui.item {
                    ui.mini.circular.image Img {
                        src = "/images/avatar2/large/elyse.png"
                    }
                    noui.content {
                        ui.sub.header { +"Elyse" }
                        +"Developer"
                    }
                }
                noui.item {
                    ui.mini.circular.image Img {
                        src = "/images/avatar2/large/eve.png"
                    }
                    noui.content {
                        ui.sub.header { +"Eve" }
                        +"Project Manager"
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderImageHeaders() = example {
        ui.header H3 { +"Image headers" }

        p { +"A header may contain an image." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderImageHeaders_1,
        ) {
            // <CodeBlock renderImageHeaders_1>
            ui.header H2 {
                ui.image Img { src = "images/icons/school.png" }
                noui.content { +"Learn More" }
            }
            shortParagraphWireFrame()
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderImageHeaders_2,
        ) {
            // <CodeBlock renderImageHeaders_2>
            ui.header H2 {
                ui.circular.image Img { src = "images/avatar2/large/patrick.png" }
                +"Patrick"
            }
            shortParagraphWireFrame()
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderImageHeaders_3,
        ) {
            // <CodeBlock renderImageHeaders_3>
            ui.header H2 {
                ui.image Img { src = "images/icons/school.png" }
                noui.content {
                    +"Plugins"
                    noui.sub.header { +"Check out our plug-in marketplace" }
                }
            }
            shortParagraphWireFrame()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIconContentHeaders() = example {
        ui.header H3 { +"Icon headers" }

        p { +"A header may contain an icon." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderIconContentHeaders_1,
        ) {
            // <CodeBlock renderIconContentHeaders_1>
            ui.header H2 {
                icon.plug()
                noui.content { +"Uptime Guarantee" }
            }
            shortParagraphWireFrame()
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderIconContentHeaders_2,
        ) {
            // <CodeBlock renderIconContentHeaders_2>
            ui.header H2 {
                icon.cogs()
                noui.content {
                    +"Account Settings"
                    noui.sub.header { +"Manage your preferences" }
                }
            }
            shortParagraphWireFrame()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSubHeaderContentHeaders() = example {
        ui.header H3 { +"Subheader" }

        p { +"Headers may contain subheaders." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderSubHeaderContentHeaders_1,
        ) {
            // <CodeBlock renderSubHeaderContentHeaders_1>
            ui.header H2 {
                +"Account Settings"
                noui.sub.header { +"Manage your preferences" }
            }
            shortParagraphWireFrame()
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderSubHeaderContentHeaders_2,
        ) {
            // <CodeBlock renderSubHeaderContentHeaders_2>
            ui.header H1 {
                +"H1"
                noui.sub.header { +"Sub Header" }
            }
            ui.header H2 {
                +"H2"
                noui.sub.header { +"Sub Header" }
            }
            ui.header H3 {
                +"H3"
                noui.sub.header { +"Sub Header" }
            }
            ui.header H4 {
                +"H4"
                noui.sub.header { +"Sub Header" }
            }
            ui.header H5 {
                +"H5"
                noui.sub.header { +"Sub Header" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabledHeaders() = example {
        ui.header H3 { +"Disabled" }

        p { +"A header can show that it is inactive." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderDisabledHeaders,
        ) {
            // <CodeBlock renderDisabledHeaders>
            ui.disabled.header H2 {
                +"Account Settings"
                noui.sub.header { +"Manage your preferences" }
            }
            shortParagraphWireFrame()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDividingHeaders() = example {
        ui.header H3 { +"Dividing headers" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderDividingHeaders,
        ) {
            // <CodeBlock renderDividingHeaders>
            shortParagraphWireFrame()

            ui.dividing.header H3 { +"This header is dividing" }

            shortParagraphWireFrame()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderBlockHeaders() = example {
        ui.header H3 { +"Block headers" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderBlockHeaders,
        ) {
            // <CodeBlock renderBlockHeaders>
            ui.block.header H3 { +"Block header" }

            mediaParagraphWireFrame()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAttachedHeaders() = example {
        ui.header H3 { +"Attached headers" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderAttachedHeaders,
        ) {
            // <CodeBlock renderAttachedHeaders>
            ui.top.attached.header { +"Top attached header" }

            ui.attached.segment {
                shortParagraphWireFrame()
            }

            ui.attached.header { +"Attached header" }

            ui.attached.segment {
                shortParagraphWireFrame()
            }

            ui.bottom.attached.header { +"Bottom attached header" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFloatingHeaders() = example {
        ui.header H3 { +"Floating headers" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderFloatingHeaders,
        ) {
            // <CodeBlock renderFloatingHeaders>
            ui.clearing.segment {
                ui.right.floated.header {
                    +"Go forward"
                }
                ui.left.floated.header {
                    +"Go backward"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTextAlignmentHeaders() = example {
        ui.header H3 { +"Floating headers" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderTextAlignmentHeaders,
        ) {
            // <CodeBlock renderTextAlignmentHeaders>
            ui.segment {
                ui.right.aligned.header { +"Right" }
                ui.left.aligned.header { +"Left" }
                ui.justified.header { +"This should take up the full width even if only one line" }
                ui.center.aligned.header { +"Center" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColoredHeaders() = example {
        ui.header H3 { +"Colored headers" }

        p { +"A header can be formatted with different colors" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderColoredHeaders,
        ) {
            // <CodeBlock renderColoredHeaders>
            ui.primary.header { +"primary" }
            ui.secondary.header { +"secondary" }

            ui.red.header { +"red" }
            ui.orange.header { +"orange" }
            ui.yellow.header { +"yellow" }
            ui.olive.header { +"olive" }
            ui.green.header { +"green" }
            ui.teal.header { +"teal" }
            ui.blue.header { +"blue" }
            ui.violet.header { +"violet" }
            ui.purple.header { +"purple" }
            ui.pink.header { +"pink" }
            ui.brown.header { +"brown" }
            ui.grey.header { +"grey" }
            ui.black.header { +"black" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInvertedHeaders() = example {
        ui.header H3 { +"Inverted headers" }

        p { +"A header can have its colors inverted for contrast" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_header_HeaderPage_kt_renderInvertedHeaders,
        ) {
            // <CodeBlock renderInvertedHeaders>
            ui.inverted.segment {
                ui.inverted.primary.header { +"primary" }
                ui.inverted.secondary.header { +"secondary" }

                ui.inverted.red.header { +"red" }
                ui.inverted.orange.header { +"orange" }
                ui.inverted.yellow.header { +"yellow" }
                ui.inverted.olive.header { +"olive" }
                ui.inverted.green.header { +"green" }
                ui.inverted.teal.header { +"teal" }
                ui.inverted.blue.header { +"blue" }
                ui.inverted.violet.header { +"violet" }
                ui.inverted.purple.header { +"purple" }
                ui.inverted.pink.header { +"pink" }
                ui.inverted.brown.header { +"brown" }
                ui.inverted.grey.header { +"grey" }
                ui.inverted.black.header { +"black" }
            }
            // </CodeBlock>
        }
    }
}
