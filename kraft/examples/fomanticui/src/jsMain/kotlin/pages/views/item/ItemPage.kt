@file:Suppress("DuplicatedCode")

package de.peekandpoke.kraft.examples.fomanticui.pages.views.item

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
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.ItemPage() = comp {
    ItemPage(it)
}

class ItemPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Views | Item")

        ui.basic.segment {
            ui.dividing.header H1 { +"Item" }

            readTheFomanticUiDocs("https://fomantic-ui.com/views/item.html")

            ui.dividing.header H2 { +"Types" }

            renderItems()

            ui.dividing.header H2 { +"Content" }

            renderContentImage()
            renderContentContent()
            renderContentHeader()
            renderContentMetadata()
            renderContentLink()
            renderContentDescription()
            renderContentExtra()

            ui.dividing.header H2 { +"Variations" }

            renderVariationsStacking()
            renderVariationsDivided()
            renderVariationsRelaxed()
            renderVariationsLinkItem()
            renderVariationsVerticalAlignment()
            renderVariationsFloated()
            renderVariationsInverted()
        }
    }

    private fun FlowContent.renderItems() = example {
        ui.header { +"Items" }

        p { +"A group of items" }

        ui.info.icon.message {
            icon.mobile()
            noui.content {
                noui.header { +"Responsive Element" }
                p { +"Item views are designed to be responsive with images stacking at mobile resolutions." }
            }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderItems,
        ) {
            // <CodeBlock renderItems>
            ui.items {
                noui.item {
                    noui.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://www.example.com"; +"Header" }
                        noui.meta { +"Meta content" }
                        noui.description { shortParagraphWireFrame() }
                        noui.extra { +"Extra details" }
                    }
                }
                noui.item {
                    noui.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://www.example.com"; +"Header" }
                        noui.meta { +"Meta content" }
                        noui.description { shortParagraphWireFrame() }
                        noui.extra { +"Extra details" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderContentImage() = example {
        ui.header { +"Image" }

        p { +"An item can contain an image" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderContentImage,
        ) {
            // <CodeBlock renderContentImage>
            ui.divided.items {
                noui.item {
                    noui.image {
                        img { src = "images/wireframe/image.png" }
                    }
                }
                noui.item {
                    noui.image {
                        img { src = "images/wireframe/image.png" }
                    }
                }
                noui.item {
                    noui.image {
                        img { src = "images/wireframe/image.png" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderContentContent() = example {
        ui.header { +"Content" }

        p { +"An item can contain content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderContentContent,
        ) {
            // <CodeBlock renderContentContent>
            ui.divided.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.middle.aligned.content {
                        +"Content"
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.middle.aligned.content {
                        +"Content"
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderContentHeader() = example {
        ui.header { +"Header" }

        p { +"An item can contain a header" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderContentHeader,
        ) {
            // <CodeBlock renderContentHeader>
            ui.divided.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.middle.aligned.content {
                        noui.header { +"Header Content" }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.middle.aligned.content {
                        noui.header { +"Header Content" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderContentMetadata() = example {
        ui.header { +"Metadata" }

        p { +"An item can contain content header" }

        ui.info.message {
            +"You can include an arbitrary amount of metadata using your own class conventions, all child "
            +"elements will automatically be spaced"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderContentMetadata,
        ) {
            // <CodeBlock renderContentMetadata>
            ui.divided.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header { +"Header Content" }
                        noui.meta {
                            span { +"$1200" }
                            span { +"1 Month" }
                        }
                        noui.description {
                            shortParagraphWireFrame()
                        }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.meta {
                            span { +"$1200" }
                            span { +"1 Month" }
                        }
                        noui.description {
                            shortParagraphWireFrame()
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderContentLink() = example {
        ui.header { +"Link" }

        p { +"An item can contain links as images, headers, or inside content" }

        ui.info.message {
            +"To make the entire content of an item link, check out the link variation below"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderContentLink,
        ) {
            // <CodeBlock renderContentLink>
            ui.divided.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            +(LoremIpsum(5) + " ")
                            a("https://example.com") { +"some link" }
                            +(" " + LoremIpsum(10))
                        }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            +(LoremIpsum(5) + " ")
                            a("https://example.com") { +"some link" }
                            +(" " + LoremIpsum(10))
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderContentDescription() = example {
        ui.header { +"Description" }

        p { +"An item can contain a description with a single or multiple paragraphs" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderContentDescription,
        ) {
            // <CodeBlock renderContentDescription>
            ui.divided.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.middle.aligned.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            p { +LoremIpsum(50) }
                            p { +LoremIpsum(20) }
                        }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.middle.aligned.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            p { +LoremIpsum(100) }
                            p { +LoremIpsum(30) }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderContentExtra() = example {
        ui.header { +"Extra Content" }

        p { +"An item can contain extra content meant to be formatted separately from the main content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderContentExtra,
        ) {
            // <CodeBlock renderContentExtra>
            ui.divided.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            p { +LoremIpsum(20) }
                        }
                        noui.extra.content {
                            icon.green.check()
                            +"121 Votes"
                        }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            p { +LoremIpsum(20) }
                        }
                        noui.extra.content {
                            icon.green.check()
                            +"121 Votes"
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsStacking() = example {
        ui.header { +"Stacking" }

        p {
            +"An item can be made "
            ui.label { +"unstackable" }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsStacking,
        ) {
            // <CodeBlock renderVariationsStacking>
            ui.unstackable.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            p { +LoremIpsum(20) }
                        }
                        noui.extra.content {
                            icon.green.check()
                            +"121 Votes"
                        }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            p { +LoremIpsum(20) }
                        }
                        noui.extra.content {
                            icon.green.check()
                            +"121 Votes"
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsDivided() = example {
        ui.header { +"Divided" }

        p { +"Items can be divided to better distinguish between grouped content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsDivided,
        ) {
            // <CodeBlock renderVariationsDivided>
            ui.divided.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            p { +LoremIpsum(20) }
                        }
                        noui.extra.content {
                            span { +"Extra" }
                            span { ui.tiny.button { +"Add" } }
                        }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                        noui.description {
                            p { +LoremIpsum(20) }
                        }
                        noui.extra.content {
                            span { +"Extra" }
                            span { ui.tiny.button { +"Add" } }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsRelaxed() = example {
        ui.header { +"Relaxed" }

        p { +"A group of items can relax its padding to provide more negative space" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsRelaxed,
        ) {
            // <CodeBlock renderVariationsRelaxed>
            ui.relaxed.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsRelaxed_2,
        ) {
            // <CodeBlock renderVariationsRelaxed_2>
            ui.very.relaxed.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsLinkItem() = example {
        ui.header { +"Link Item" }

        p { +"An item can be formatted so that the entire contents link to another page" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsLinkItem_1,
        ) {
            // <CodeBlock renderVariationsLinkItem_1>
            ui.link.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header { +"Header Content" }
                        noui.description { shortParagraphWireFrame() }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header { +"Header Content" }
                        noui.description { shortParagraphWireFrame() }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsLinkItem_2,
        ) {
            // <CodeBlock renderVariationsLinkItem_2>
            ui.very.relaxed.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header A { href = "https://example.com"; +"Header Content" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsVerticalAlignment() = example {
        ui.header { +"Vertical alignment" }

        p { +"Content can specify its vertical alignment" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsVerticalAlignment,
        ) {
            // <CodeBlock renderVariationsVerticalAlignment>
            ui.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header { +"Top Aligned" }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.middle.aligned.content {
                        noui.header { +"Middle Aligned" }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.bottom.aligned.content {
                        noui.header { +"Bottom Aligned" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsFloated() = example {
        ui.header { +"Floated" }

        p { +"Any content element can be floated left or right" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsFloated,
        ) {
            // <CodeBlock renderVariationsFloated>
            ui.items {
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header { +"Top Aligned" }
                        noui.description { shortParagraphWireFrame() }
                        noui.extra.content {
                            ui.right.floated.button { +"Right floated" }
                        }
                    }
                }
                noui.item {
                    ui.tiny.image {
                        img { src = "images/wireframe/image.png" }
                    }
                    noui.content {
                        noui.header { +"Top Aligned" }
                        noui.description { shortParagraphWireFrame() }
                        noui.extra.content {
                            ui.right.floated.button { +"Right floated" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsInverted() = example {
        ui.header { +"Inverted" }

        p { +"An item can be inverted to appear on a dark background" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_item_ItemPage_kt_renderVariationsInverted,
        ) {
            // <CodeBlock renderVariationsInverted>
            ui.inverted.segment {
                ui.inverted.items {
                    noui.item {
                        ui.tiny.image {
                            img { src = "images/wireframe/image.png" }
                        }
                        noui.content {
                            noui.header { +"Top Aligned" }
                            noui.meta { +"Meta" }
                            noui.description { shortParagraphWireFrame() }
                            noui.extra.content { +"Extra" }
                        }
                    }
                    noui.item {
                        ui.tiny.image {
                            img { src = "images/wireframe/image.png" }
                        }
                        noui.content {
                            noui.header { +"Top Aligned" }
                            noui.meta { +"Meta" }
                            noui.description { shortParagraphWireFrame() }
                            noui.extra.content { +"Extra" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }
}
