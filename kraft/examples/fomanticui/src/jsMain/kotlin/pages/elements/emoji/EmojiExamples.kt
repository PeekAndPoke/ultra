@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.elements.emoji

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.emoji
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.classes
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.EmojiExamples() = comp {
    EmojiExamples(it)
}

class EmojiExamples(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.dividing.header { +"Types" }

        renderEmoji()

        ui.dividing.header { +"States" }

        renderDisabled()
        renderLoading()

        ui.dividing.header { +"Variations" }

        renderSize()
        renderLink()
    }

    private fun FlowContent.renderEmoji() = example {
        ui.header H3 { +"Emoji" }

        p { +"An emoji can be rendered by providing a shortname." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_emoji_EmojiExamples_kt_renderEmoji,
        ) {
            // <CodeBlock renderEmoji>
            ui.segment {
                emoji("thinking")
                emoji("grinning")
                emoji("heart")
                emoji("rocket")
                emoji("fire")
                emoji("thumbsup")
                emoji("star")
                emoji("sun")
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabled() = example {
        ui.header H3 { +"Disabled" }

        p { +"An emoji can show it is disabled." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_emoji_EmojiExamples_kt_renderDisabled,
        ) {
            // <CodeBlock renderDisabled>
            ui.segment {
                emoji("thinking") { classes += "disabled" }
                emoji("grinning") { classes += "disabled" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLoading() = example {
        ui.header H3 { +"Loading" }

        p { +"An emoji can be used as a simple loader." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_emoji_EmojiExamples_kt_renderLoading,
        ) {
            // <CodeBlock renderLoading>
            ui.segment {
                emoji("thinking") { classes += "loading" }
                emoji("rocket") { classes += "loading" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"An emoji can vary in size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_emoji_EmojiExamples_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            ui.segment {
                emoji("rocket") { classes += "small" }
                emoji("rocket")
                emoji("rocket") { classes += "large" }
                emoji("rocket") { classes += "big" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLink() = example {
        ui.header H3 { +"Link" }

        p { +"An emoji can be formatted as a link." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_emoji_EmojiExamples_kt_renderLink,
        ) {
            // <CodeBlock renderLink>
            ui.segment {
                emoji("thinking") { classes += "link" }
                emoji("grinning") { classes += "link" }
                emoji("heart") { classes += "link" }
            }
            // </CodeBlock>
        }
    }
}
