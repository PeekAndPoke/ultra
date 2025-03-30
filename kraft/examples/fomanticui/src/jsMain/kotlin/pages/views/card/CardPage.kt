@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.views.card

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.examples.fomanticui.helpers.shortParagraphWireFrame
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.CardPage() = comp {
    CardPage(it)
}

class CardPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Views | Card")

        ui.basic.segment {
            ui.dividing.header H1 { +"Card" }

            readTheFomanticUiDocs("https://fomantic-ui.com/views/card.html")

            ui.dividing.header H2 { +"Types" }

            renderSingleCard()
            renderCardGroup()

            ui.dividing.header H2 { +"Loading" }

            renderLoadingCards()

            ui.dividing.header H2 { +"Variations" }

            renderFluidCards()
            renderCenteredCard()
            renderHorizontalCards()
            renderRaisedCards()
            renderTextAlignment()
            renderInvertedCards()
            renderColoredCards()
            renderColumnCountCards()
            renderStackableCards()
            renderDoublingCards()
        }
    }

    private fun FlowContent.renderSingleCard() = example {
        ui.header { +"Single card" }

        ui.stackable.two.column.grid {
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderSingleCard,
                ) {
                    // <CodeBlock renderSingleCard>
                    ui.card {
                        noui.image {
                            img(src = "images/avatar2/large/kristy.png")
                        }
                        noui.content {
                            noui.header A { +"Kristy" }
                            noui.meta {
                                +"Joined in 2022"
                            }
                            noui.description {
                                +"Kristy is an art director living in Leipzig."
                            }
                        }
                        noui.extra.content {
                            a {
                                icon.user()
                                +"22 Friends"
                            }
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderSingleCard2,
                ) {
                    // <CodeBlock renderSingleCard2>
                    ui.card {
                        noui.content {
                            noui.right.floated.meta { +"14h" }
                            ui.avatar.image Img {
                                src = "images/avatar2/large/elliot.jpg"
                                +"Elliot"
                            }
                        }
                        noui.image {
                            img(src = "images/wireframe/image.png")
                        }
                        noui.content {
                            noui.right.floated Span {
                                icon.heart_outline()
                                +"17 Likes"
                            }
                            icon.comment()
                            +"3 comments"
                        }
                        noui.extra.content {
                            ui.large.with("transparent").left.icon.input {
                                icon.heart_outline()
                                input {
                                    type = InputType.text
                                    placeholder = "Add comment..."
                                }
                            }
                        }
                    }
                    // </CodeBlock>
                }
            }
        }
    }

    private fun FlowContent.renderCardGroup() = example {
        ui.header { +"Card groups" }

        p { +"Cards in the same group have the same height" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderCardGroup_1,
        ) {
            // <CodeBlock renderCardGroup_1>
            ui.cards {
                noui.card {
                    noui.content {
                        ui.right.floated.mini.image Img {
                            src = "images/avatar2/large/elliot.jpg"
                        }
                        noui.header { +"Elliot Fu" }
                        noui.meta { +"Friends of Veronika" }
                        noui.description { +"Elliot requested permission to view your contact details" }
                    }
                    noui.extra.content {
                        ui.two.buttons {
                            ui.basic.green.button { +"Approve" }
                            ui.basic.red.button { +"Decline" }
                        }
                    }
                }
                noui.card {
                    noui.content {
                        ui.right.floated.mini.image Img {
                            src = "images/avatar2/large/jenny.jpg"
                        }
                        noui.header { +"Jenny Hess" }
                        noui.meta { +"New member" }
                        noui.description {
                            +"Jenny wants to add you to the group "
                            b { +"best friends" }
                        }
                    }
                    noui.extra.content {
                        ui.two.buttons {
                            ui.basic.green.button { +"Approve" }
                            ui.basic.red.button { +"Decline" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }

        p { +"Cards can be linked" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderCardGroup_2,
        ) {
            // <CodeBlock renderCardGroup_2>
            ui.link.cards {
                noui.card {
                    noui.image {
                        img(src = "images/avatar2/large/matthew.png")
                    }
                    noui.content {
                        noui.header { +"Matt Giampietro" }
                        noui.meta { a { +"Friends" } }
                        noui.description { +"Matthew is an interior designer living in New York." }
                    }
                    noui.extra.content {
                        noui.right.floated Span { +"Joined in 2013" }
                        span { icon.user(); +"75 friends" }
                    }
                }
                noui.card {
                    noui.image {
                        img(src = "images/avatar2/large/molly.png")
                    }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.meta { a { +"Coworker" } }
                        noui.description { +"Molly is a personal assistant living in Paris." }
                    }
                    noui.extra.content {
                        noui.right.floated Span { +"Joined in 2014" }
                        span { icon.user(); +"35 friends" }
                    }
                }
                noui.card {
                    noui.image {
                        img(src = "images/avatar2/large/elyse.png")
                    }
                    noui.content {
                        noui.header { +"Elyse" }
                        noui.meta { a { +"Coworker" } }
                        noui.description { +"Elyse is a copywriter working in New York." }
                    }
                    noui.extra.content {
                        noui.right.floated Span { +"Joined in 2014" }
                        span { icon.user(); +"151 friends" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLoadingCards() = example {
        ui.header { +"Loading card" }

        ui.stackable.two.column.grid {
            ui.column {
                p { +"A card may show its content is being loaded" }

                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderLoadingCards_1,
                ) {
                    // <CodeBlock renderLoadingCards_1>
                    ui.loading.card {
                        noui.content {
                            noui.header A { +"Kristy" }
                            noui.meta {
                                +"Joined in 2022"
                            }
                            noui.description {
                                +"Kristy is an art director living in Leipzig."
                            }
                        }
                        noui.extra.content {
                            a {
                                icon.user()
                                +"22 Friends"
                            }
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                p {
                    +"The loader inherits the color of the card, if you want to prevent this, add the "
                    ui.basic.label Span { +"usual" }
                    +" class, so the loader color stays default, while the card still gets its color"
                }

                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderLoadingCards_2,
                ) {
                    ui.cards {
                        // <CodeBlock renderLoadingCards_2>
                        ui.red.loading.card {
                            noui.content {
                                noui.header A { +"Kristy" }
                                noui.meta { +"Joined in 2022" }
                            }
                        }
                        ui.green.usual.loading.card {
                            noui.content {
                                noui.header A { +"Kristy" }
                                noui.meta { +"Joined in 2022" }
                            }
                        }
                        // </CodeBlock>
                    }
                }
            }
        }
    }

    private fun FlowContent.renderFluidCards() = example {
        ui.header { +"Fluid cards" }

        p { +"A fluid card takes up the width of its container" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderFluidCards_1,
        ) {
            // <CodeBlock renderFluidCards_1>
            ui.three.column.grid {
                ui.column {
                    ui.fluid.card {
                        noui.image {
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.header { +"Matt Giampietro" }
                        }
                    }
                }
                ui.column {
                    ui.fluid.card {
                        noui.image {
                            img(src = "images/avatar2/large/molly.png")
                        }
                        noui.content {
                            noui.header { +"Molly" }
                        }
                    }
                }
                ui.column {
                    ui.fluid.card {
                        noui.image {
                            img(src = "images/avatar2/large/elyse.png")
                        }
                        noui.content {
                            noui.header { +"Elyse" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCenteredCard() = example {
        ui.header { +"Fluid cards" }

        p { +"A card can center itself inside its container" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderCenteredCard,
        ) {
            // <CodeBlock renderCenteredCard>
            ui.centered.card {
                noui.image {
                    img(src = "images/avatar2/large/matthew.png")
                }
                noui.content {
                    noui.header { +"Matt Giampietro" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHorizontalCards() = example {
        ui.header { +"Fluid cards" }

        p { +"A card can display horizontally" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderHorizontalCards_1,
        ) {
            // <CodeBlock renderHorizontalCards_1>
            ui.horizontal.card {
                noui.image {
                    img(src = "images/avatar2/large/matthew.png")
                }
                noui.content {
                    noui.header { +"Matt Giampietro" }
                }
            }
            // </CodeBlock>
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderHorizontalCards_2,
        ) {
            // <CodeBlock renderHorizontalCards_2>
            ui.cards {
                ui.horizontal.card {
                    noui.image {
                        img(src = "images/avatar2/large/matthew.png")
                    }
                    noui.content {
                        noui.header { +"Matt Giampietro" }
                        noui.meta { a { +"Friends" } }
                        noui.description { +"Matthew is an interior designer living in New York." }
                    }
                    noui.extra.content {
                        noui.right.floated Span { +"Joined in 2013" }
                        span { icon.user(); +"75 friends" }
                    }
                }
                ui.horizontal.card {
                    noui.image {
                        img(src = "images/avatar2/large/molly.png")
                    }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.meta { a { +"Coworker" } }
                        noui.description { +"Molly is a personal assistant living in Paris." }
                    }
                    noui.extra.content {
                        noui.right.floated Span { +"Joined in 2014" }
                        span { icon.user(); +"35 friends" }
                    }
                }
                ui.horizontal.card {
                    noui.image {
                        img(src = "images/avatar2/large/elyse.png")
                    }
                    noui.content {
                        noui.header { +"Elyse" }
                        noui.meta { a { +"Coworker" } }
                        noui.description { +"Elyse is a copywriter working in New York." }
                    }
                    noui.extra.content {
                        noui.right.floated Span { +"Joined in 2014" }
                        span { icon.user(); +"151 friends" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRaisedCards() = example {
        ui.header { +"Raised cards" }

        p { +"A card may be formatted to raise above the page." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderRaisedCards_1,
        ) {
            // <CodeBlock renderRaisedCards_1>
            ui.raised.card {
                noui.content {
                    noui.header { +"Cute dog" }
                    noui.meta { +"Animals" }
                    noui.description {
                        shortParagraphWireFrame()
                    }
                }
                noui.extra.content {
                    noui.right.floated {
                        ui.avatar.image Img { src = "images/avatar2/large/matthew.png" }
                        +"Matt"
                    }
                }
            }
            // </CodeBlock>
        }

        p { +"A raised card can be linked." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderRaisedCards_2,
        ) {
            // <CodeBlock renderRaisedCards_2>
            ui.raised.link.card {
                noui.content {
                    noui.header { +"Cute dog" }
                    noui.meta { +"Animals" }
                    noui.description {
                        shortParagraphWireFrame()
                    }
                }
                noui.extra.content {
                    noui.right.floated {
                        ui.avatar.image Img { src = "images/avatar2/large/matthew.png" }
                        +"Matt"
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTextAlignment() = example {
        ui.header { +"Text alignment" }

        p { +"Any element inside a card can have its text alignment specified." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderTextAlignment,
        ) {
            // <CodeBlock renderTextAlignment>
            ui.card {
                noui.center.aligned.content {
                    noui.header { +"Cute dog" }
                    noui.meta { +"Animals" }
                    noui.description {
                        shortParagraphWireFrame()
                    }
                }
                noui.center.aligned.extra.content {
                    ui.avatar.image Img { src = "images/avatar2/large/matthew.png" }
                    +"Matt"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInvertedCards() = example {
        ui.header { +"Inverted" }

        p { +"Card's colors can be inverted" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderInvertedCards_1,
        ) {
            // <CodeBlock renderInvertedCards_1>
            ui.inverted.segment {
                ui.inverted.card {
                    noui.content {
                        ui.right.floated.mini.image Img {
                            src = "images/avatar2/large/elliot.jpg"
                        }
                        noui.header { +"Elliot Fu" }
                        noui.meta { +"Friends of Veronika" }
                        noui.description { +"Elliot requested permission to view your contact details" }
                    }
                    noui.extra.content {
                        ui.two.buttons {
                            ui.basic.green.button { +"Approve" }
                            ui.basic.red.button { +"Decline" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderInvertedCards_2,
        ) {
            // <CodeBlock renderInvertedCards_2>
            ui.inverted.segment {
                ui.inverted.link.cards {
                    noui.card {
                        noui.image {
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.header { +"Matt Giampietro" }
                            noui.meta { a { +"Friends" } }
                            noui.description { +"Matthew is an interior designer living in New York." }
                        }
                        noui.extra.content {
                            noui.right.floated Span { +"Joined in 2013" }
                            span { icon.user(); +"75 friends" }
                        }
                    }
                    noui.card {
                        noui.image {
                            img(src = "images/avatar2/large/molly.png")
                        }
                        noui.content {
                            noui.header { +"Molly" }
                            noui.meta { a { +"Coworker" } }
                            noui.description { +"Molly is a personal assistant living in Paris." }
                        }
                        noui.extra.content {
                            noui.right.floated Span { +"Joined in 2014" }
                            span { icon.user(); +"35 friends" }
                        }
                    }
                    noui.card {
                        noui.image {
                            img(src = "images/avatar2/large/elyse.png")
                        }
                        noui.content {
                            noui.header { +"Elyse" }
                            noui.meta { a { +"Coworker" } }
                            noui.description { +"Elyse is a copywriter working in New York." }
                        }
                        noui.extra.content {
                            noui.right.floated Span { +"Joined in 2014" }
                            span { icon.user(); +"151 friends" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColoredCards() = example {
        ui.header { +"Colored" }

        p { +"A card can specify a color" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderColoredCards_1,
        ) {
            // <CodeBlock renderColoredCards_1>
            ui.eight.cards {
                noui.primary.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"primary" } }
                }
                noui.secondary.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"secondary" } }
                }
                noui.red.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"red" } }
                }
                noui.orange.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"primary" } }
                }
                noui.yellow.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"yellow" } }
                }
                noui.olive.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"olive" } }
                }
                noui.green.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"green" } }
                }
                noui.teal.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"teal" } }
                }
                noui.blue.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"blue" } }
                }
                noui.violet.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"violet" } }
                }
                noui.purple.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"purple" } }
                }
                noui.pink.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"pink" } }
                }
                noui.brown.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"brown" } }
                }
                noui.grey.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"grey" } }
                }
                noui.black.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"black" } }
                }
                noui.white.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"white" } }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColumnCountCards() = example {
        ui.header { +"Column count" }

        p { +"A group of cards can set how many cards should exist in a row." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderColumnCountCards_1,
        ) {
            // <CodeBlock renderColumnCountCards_1>
            ui.five.cards {
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderStackableCards() = example {
        ui.header { +"Stackable" }

        p { +"A group of cards can automatically stack rows to a single columns on mobile devices" }

        ui.yellow.message {
            +"Resize your browser to a smaller size to see the cards stack after reaching mobile breakpoints"
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderStackableCards,
        ) {
            // <CodeBlock renderStackableCards>
            ui.six.stackable.cards {
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDoublingCards() = example {
        ui.header { +"Doubling" }

        p { +"A group of cards can double its column width for mobile" }

        ui.yellow.message {
            +"Resize your browser to a smaller size to see the cards stack after reaching mobile breakpoints"
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_card_CardPage_kt_renderDoublingCards,
        ) {
            // <CodeBlock renderDoublingCards>
            ui.six.doubling.cards {
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
                noui.card {
                    noui.image { img { src = "images/wireframe/white-image.png" } }
                    noui.content { noui.header { +"..." } }
                }
            }
            // </CodeBlock>
        }
    }
}
