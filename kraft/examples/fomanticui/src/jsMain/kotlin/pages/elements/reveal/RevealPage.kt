@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.reveal

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.RevealPage() = comp {
    RevealPage(it)
}

class RevealPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Reveal")

        ui.basic.segment {
            ui.dividing.header H1 { +"Reveal" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/reveal.html")

            ui.dividing.header H2 { +"Types" }

            renderFade()
            renderMove()
            renderRotate()

            ui.dividing.header H2 { +"Variations" }

            renderInstant()

            ui.dividing.header H2 { +"States" }

            renderActive()
            renderDisabled()
        }
    }

    private fun FlowContent.renderFade() = example {
        ui.dividing.header H3 { +"Fade" }

        p {
            +"An element can disappear to reveal content below"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderFade,
        ) {
            // <CodeBlock renderFade>
            ui.fade.reveal {
                noui.visible.content {
                    ui.small.image Img { src = "images/wireframe/square-image.png" }
                }
                noui.hidden.content {
                    ui.small.image Img { src = "images/avatar2/large/elliot.jpg" }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderFade_2,
        ) {
            // <CodeBlock renderFade_2>
            ui.small.fade.reveal.image {
                noui.visible.content Img { src = "images/wireframe/square-image.png" }
                noui.hidden.content Img { src = "images/avatar2/large/eve.png" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderMove() = example {
        ui.dividing.header H3 { +"Move" }

        p {
            +"An element can move in a direction to reveal content"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderMove_1,
        ) {
            // <CodeBlock renderMove_1>
            ui.move.reveal {
                noui.visible.content {
                    ui.small.image Img { src = "images/wireframe/square-image.png" }
                }
                noui.hidden.content {
                    ui.small.image Img { src = "images/avatar2/large/elliot.jpg" }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderMove_2,
        ) {
            // <CodeBlock renderMove_2>
            ui.basic.fitted.compact.segment {
                ui.move.right.reveal {
                    noui.visible.content {
                        ui.small.image Img { src = "images/wireframe/square-image.png" }
                    }
                    noui.hidden.content {
                        ui.small.image Img { src = "images/avatar2/large/eve.png" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderMove_3,
        ) {
            // <CodeBlock renderMove_3>
            ui.move.up.reveal {
                noui.visible.content {
                    ui.small.image Img { src = "images/wireframe/square-image.png" }
                }
                noui.hidden.content {
                    ui.small.image Img { src = "images/avatar2/large/molly.png" }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderMove_4,
        ) {
            // <CodeBlock renderMove_4>
            ui.move.down.reveal {
                noui.visible.content {
                    ui.small.image Img { src = "images/wireframe/square-image.png" }
                }
                noui.hidden.content {
                    ui.small.image Img { src = "images/avatar2/large/matthew.png" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRotate() = example {
        ui.dividing.header H3 { +"Rotate" }

        p {
            +"An element can rotate to reveal content below"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderRotate_1,
        ) {
            // <CodeBlock renderRotate_1>
            ui.small.circular.rotate.reveal.image {
                noui.visible.content Img { src = "images/wireframe/square-image.png" }
                noui.hidden.content Img { src = "images/avatar2/large/eve.png" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderRotate_2,
        ) {
            // <CodeBlock renderRotate_2>
            ui.small.circular.rotate.left.reveal.image {
                noui.visible.content Img { src = "images/wireframe/square-image.png" }
                noui.hidden.content Img { src = "images/avatar2/large/kristy.png" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInstant() = example {
        ui.dividing.header H3 { +"Instant" }

        p {
            +"An element can show its content without delay"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderInstant,
        ) {
            // <CodeBlock renderInstant>
            ui.small.active.instant.reveal.image {
                noui.visible.content Img { src = "images/avatar2/large/eve.png" }
                noui.hidden.content Img { src = "images/wireframe/square-image.png" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderActive() = example {
        ui.dividing.header H3 { +"Active" }

        ui.info.message {
            +"Adding the class "
            ui.label { +"active" }
            +" can allow you to show the hidden contents programmatically "
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderActive,
        ) {
            // <CodeBlock renderActive>
            ui.small.active.reveal.image {
                noui.visible.content Img { src = "images/avatar2/large/eve.png" }
                noui.hidden.content Img { src = "images/wireframe/square-image.png" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabled() = example {
        ui.dividing.header H3 { +"Disabled" }

        p {
            +"A disabled reveal will not animate when hovered"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_reveal_RevealPage_kt_renderDisabled,
        ) {
            // <CodeBlock renderDisabled>
            ui.disabled.move.reveal {
                noui.visible.content {
                    ui.small.image Img { src = "images/wireframe/square-image.png" }
                }
                noui.hidden.content {
                    ui.small.image Img { src = "images/avatar2/large/elliot.jpg" }
                }
            }
            // </CodeBlock>
        }
    }
}
