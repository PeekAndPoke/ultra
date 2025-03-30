package de.peekandpoke.kraft.examples.fomanticui.pages.collections.message

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.mediaParagraphWireFrame
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.li
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.MessagePage() = comp {
    MessagePage(it)
}

class MessagePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Collections | Message")

        ui.basic.segment {
            ui.dividing.header H1 { +"Message" }

            readTheFomanticUiDocs("https://fomantic-ui.com/collections/message.html")

            ui.dividing.header H2 { +"Types" }

            renderMessage()
            renderListMessage()
            renderIconMessage()

            ui.dividing.header H2 { +"States" }

            renderHiddenState()
            renderVisibleState()

            ui.dividing.header H2 { +"Variations" }

            renderCenterAligned()
            renderRightAligned()
            renderFloating()
            renderCompact()
            renderAttached()
            renderWarning()
            renderInfo()
            renderPositiveOrSuccess()
            renderNegativeOrError()
            renderColored()
            renderSize()
        }
    }

    private fun FlowContent.renderMessage() = example {
        ui.header H3 { +"Message" }

        p { +"A basic message" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderMessage,
        ) {
            // <CodeBlock renderMessage>
            ui.message {
                ui.header { +"Changes in Service" }
                p { +"We just updated our privacy policy here to better service our customers." }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderListMessage() = example {
        ui.header H3 { +"List Message" }

        p { +"A message with a list" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderListMessage,
        ) {
            // <CodeBlock renderListMessage>
            ui.message {
                ui.header { +"New Site Features" }
                noui.list Ul {
                    li { +"You can now have cover images on blog pages" }
                    li { +"Drafts will now auto-save while writing" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIconMessage() = example {
        ui.header H3 { +"Icon Message" }

        p { +"A message can contain an icon." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderIconMessage_1,
        ) {
            // <CodeBlock renderIconMessage_1>
            ui.icon.message {
                icon.inbox()
                noui.content {
                    noui.header { +"Have you heard about our mailing list?" }
                    p { +"Get the best news in your e-mail every day." }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderIconMessage_2,
        ) {
            // <CodeBlock renderIconMessage_2>
            ui.icon.message {
                icon.loading.circle_notch()
                noui.content {
                    noui.header { +"Just one second " }
                    p { +"We're fetching that content for you." }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHiddenState() = example {
        ui.header H3 { +"Hidden" }

        p { +"A message can be hidden" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderHiddenState,
        ) {
            // <CodeBlock renderHiddenState>
            ui.hidden.message {
                +"You can't see me"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVisibleState() = example {
        ui.header H3 { +"Visible" }

        p { +"A message can be set to visible to force itself to be shown." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderVisibleState,
        ) {
            // <CodeBlock renderVisibleState>
            ui.visible.message {
                +"You can always see me"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCenterAligned() = example {
        ui.header H3 { +"Center Aligned" }

        p { +"A message can be displayed center aligned" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderCenterAligned,
        ) {
            // <CodeBlock renderCenterAligned>
            ui.center.aligned.message {
                noui.content {
                    noui.header { +"Center aligned" }
                    p { +"The content also center aligned" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRightAligned() = example {
        ui.header H3 { +"Right Aligned" }

        p { +"A message can be displayed right aligned" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderRightAligned,
        ) {
            // <CodeBlock renderRightAligned>
            ui.right.aligned.message {
                noui.content {
                    noui.header { +"Right aligned" }
                    p { +"The content also right aligned" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFloating() = example {
        ui.header H3 { +"Floating" }

        p { +"A message can float above content that it is related to" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderFloating,
        ) {
            // <CodeBlock renderFloating>
            ui.floating.message {
                +"This message is floating / raised."
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCompact() = example {
        ui.header H3 { +"Compact" }

        p { +"A message can only take up the width of its content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderCompact,
        ) {
            // <CodeBlock renderCompact>
            ui.compact.message {
                +"Get all the best inventions in your e-mail every day. Sign up now!"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAttached() = example {
        ui.header H3 { +"Attached" }

        p { +"A message can be formatted to attach itself to other content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderAttached,
        ) {
            // <CodeBlock renderAttached>
            ui.top.attached.message {
                +"This message is top attached"
            }
            ui.attached.segment {
                mediaParagraphWireFrame()
            }
            ui.bottom.attached.warning.message {
                icon.question()
                +"Any questions?"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderWarning() = example {
        ui.header H3 { +"Warning" }

        p { +"A message may be formatted to display warning messages." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderWarning,
        ) {
            // <CodeBlock renderWarning>
            ui.warning.message {
                noui.header { +"This is a warning message" }
                p { +"This is some content" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInfo() = example {
        ui.header H3 { +"Info" }

        p { +"A message may be formatted to display information." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderInfo,
        ) {
            // <CodeBlock renderInfo>
            ui.info.message {
                noui.header { +"This is an info message" }
                p { +"This is some content" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderPositiveOrSuccess() = example {
        ui.header H3 { +"Positive / Success" }

        p { +"A message may be formatted to display a positive message." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderPositiveOrSuccess_1,
        ) {
            // <CodeBlock renderPositiveOrSuccess_1>
            ui.positive.message {
                noui.header { +"This is a positive message" }
                p { +"This is some content" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderPositiveOrSuccess_2,
        ) {
            // <CodeBlock renderPositiveOrSuccess_2>
            ui.success.message {
                noui.header { +"This is a success message" }
                p { +"This is some content" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderNegativeOrError() = example {
        ui.header H3 { +"Negative / Error" }

        p { +"A message may be formatted to display a negative message." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderNegativeOrError_1,
        ) {
            // <CodeBlock renderNegativeOrError_1>
            ui.negative.message {
                noui.header { +"This is a negative message" }
                p { +"This is some content" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderNegativeOrError_2,
        ) {
            // <CodeBlock renderNegativeOrError_2>
            ui.error.message {
                noui.header { +"This is an error message" }
                p { +"This is some content" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColored() = example {
        ui.header H3 { +"Colored" }

        p { +"A message can be formatted to be different colors" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderColored,
        ) {
            // <CodeBlock renderColored>
            ui.red.message { +"red" }
            ui.orange.message { +"orange" }
            ui.yellow.message { +"yellow" }
            ui.olive.message { +"olive" }
            ui.green.message { +"green" }
            ui.teal.message { +"teal" }
            ui.blue.message { +"blue" }
            ui.violet.message { +"violet" }
            ui.purple.message { +"purple" }
            ui.pink.message { +"pink" }
            ui.brown.message { +"brown" }
            ui.grey.message { +"grey" }
            ui.black.message { +"black" }
            ui.white.message { +"white" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"A message can have different sizes" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_message_MessagePage_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            ui.mini.message { +"mini" }
            ui.tiny.message { +"tiny" }
            ui.small.message { +"small" }
            ui.medium.message { +"medium" }
            ui.large.message { +"large" }
            ui.big.message { +"big" }
            ui.huge.message { +"huge" }
            ui.massive.message { +"massive" }
            // </CodeBlock>
        }
    }
}
