@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.views.feed

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.img
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.FeedPage() = comp {
    FeedPage(it)
}

class FeedPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Views | Feed")

        ui.basic.segment {
            ui.dividing.header H1 { +"Feed" }

            readTheFomanticUiDocs("https://fomantic-ui.com/views/feed.html")

            ui.dividing.header H2 { +"Types" }

            renderFeed()

            ui.dividing.header H2 { +"Content" }

            renderImageLabel()
            renderIconLabel()
            renderDate()
            renderAdditionalInformation()

            ui.dividing.header H2 { +"States" }

            renderDisabled()

            ui.dividing.header H2 { +"Variations" }

            renderConnected()
            renderSize()
            renderInverted()
        }
    }

    private fun FlowContent.renderFeed() = example {
        ui.header H3 { +"Feed" }

        p { +"A feed." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderFeed,
        ) {
            // <CodeBlock renderFeed>
            ui.feed {
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/elliot.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Elliot Fu" }
                            +" added you as a friend"
                            noui.date { +"1 Hour Ago" }
                        }
                        noui.meta {
                            noui.like { icon.heart(); +"4 Likes" }
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/helen.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Helen Troy" }
                            +" added "
                            a { +"2 new illustrations" }
                            noui.date { +"4 days ago" }
                        }
                        noui.meta {
                            noui.like { icon.heart(); +"1 Like" }
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/jenny.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Jenny Hess" }
                            +" added you as a friend"
                            noui.date { +"2 Days Ago" }
                        }
                        noui.meta {
                            noui.like { icon.heart(); +"8 Likes" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderImageLabel() = example {
        ui.header H3 { +"Image Label" }

        p { +"An event can contain an image label." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderImageLabel,
        ) {
            // <CodeBlock renderImageLabel>
            ui.feed {
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/elliot.jpg")
                    }
                    noui.content {
                        noui.summary {
                            +"You added "
                            a { +"Elliot Fu" }
                            +" to your "
                            a { +"Musicians" }
                            +" group."
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIconLabel() = example {
        ui.header H3 { +"Icon Label" }

        p { +"An event can contain an icon label." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderIconLabel,
        ) {
            // <CodeBlock renderIconLabel>
            ui.feed {
                noui.event {
                    noui.label {
                        icon.pencil_alternate()
                    }
                    noui.content {
                        noui.summary {
                            a { +"You" }
                            +" posted on your friend "
                            a { +"Stevie's" }
                            +" wall."
                            noui.date { +"Today" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDate() = example {
        ui.header H3 { +"Date" }

        p { +"An event can contain a date." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderDate,
        ) {
            // <CodeBlock renderDate>
            ui.feed {
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/jenny.jpg")
                    }
                    noui.content {
                        noui.date { +"3 days ago" }
                        noui.summary {
                            a { +"Jenny Hess" }
                            +" added you as a friend"
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/elliot.jpg")
                    }
                    noui.content {
                        noui.date { +"1 hour ago" }
                        noui.summary {
                            a { +"Elliot Fu" }
                            +" added you as a friend"
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAdditionalInformation() = example {
        ui.header H3 { +"Additional Information" }

        p { +"An event can contain additional information like extra text or images." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderAdditionalInformation,
        ) {
            // <CodeBlock renderAdditionalInformation>
            ui.feed {
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/elliot.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Elliot Fu" }
                            +" added you as a friend"
                            noui.date { +"1 Hour Ago" }
                        }
                        noui.extra.text {
                            +"Elliot has been a member since July 2024."
                        }
                        noui.meta {
                            noui.like { icon.heart(); +"4 Likes" }
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/helen.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Helen Troy" }
                            +" added "
                            a { +"2 new illustrations" }
                            noui.date { +"4 days ago" }
                        }
                        noui.extra.images {
                            a { img(src = "images/wireframe/image.png") }
                            a { img(src = "images/wireframe/image.png") }
                        }
                        noui.meta {
                            noui.like { icon.heart(); +"1 Like" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabled() = example {
        ui.header H3 { +"Disabled" }

        p { +"A feed or event can be disabled." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderDisabled,
        ) {
            // <CodeBlock renderDisabled>
            ui.feed {
                noui.disabled.event {
                    noui.label {
                        img(src = "images/avatar2/large/elliot.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Elliot Fu" }
                            +" added you as a friend"
                            noui.date { +"1 Hour Ago" }
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/helen.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Helen Troy" }
                            +" added "
                            a { +"2 new illustrations" }
                            noui.date { +"4 days ago" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderConnected() = example {
        ui.header H3 { +"Connected" }

        p { +"A feed can show events connected via a timeline." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderConnected,
        ) {
            // <CodeBlock renderConnected>
            ui.connected.feed {
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/elliot.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Elliot Fu" }
                            +" added you as a friend"
                            noui.date { +"1 Hour Ago" }
                        }
                        noui.meta {
                            noui.like { icon.heart(); +"4 Likes" }
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/helen.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Helen Troy" }
                            +" added "
                            a { +"2 new illustrations" }
                            noui.date { +"4 days ago" }
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/jenny.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Jenny Hess" }
                            +" added you as a friend"
                            noui.date { +"2 Days Ago" }
                        }
                        noui.meta {
                            noui.like { icon.heart(); +"8 Likes" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"A feed can have different sizes." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            ui.header H4 { +"Small" }
            ui.small.feed {
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/elliot.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Elliot Fu" }
                            +" added you as a friend"
                            noui.date { +"1 Hour Ago" }
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/helen.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Helen Troy" }
                            +" added "
                            a { +"2 new illustrations" }
                            noui.date { +"4 days ago" }
                        }
                    }
                }
            }

            ui.header H4 { +"Large" }
            ui.large.feed {
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/elliot.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Elliot Fu" }
                            +" added you as a friend"
                            noui.date { +"1 Hour Ago" }
                        }
                    }
                }
                noui.event {
                    noui.label {
                        img(src = "images/avatar2/large/helen.jpg")
                    }
                    noui.content {
                        noui.summary {
                            a { +"Helen Troy" }
                            +" added "
                            a { +"2 new illustrations" }
                            noui.date { +"4 days ago" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.header H3 { +"Inverted" }

        p { +"A feed can have inverted colors." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_feed_FeedPage_kt_renderInverted,
        ) {
            // <CodeBlock renderInverted>
            ui.inverted.segment {
                ui.inverted.feed {
                    noui.event {
                        noui.label {
                            img(src = "images/avatar2/large/elliot.jpg")
                        }
                        noui.content {
                            noui.summary {
                                a { +"Elliot Fu" }
                                +" added you as a friend"
                                noui.date { +"1 Hour Ago" }
                            }
                            noui.meta {
                                noui.like { icon.heart(); +"4 Likes" }
                            }
                        }
                    }
                    noui.event {
                        noui.label {
                            img(src = "images/avatar2/large/helen.jpg")
                        }
                        noui.content {
                            noui.summary {
                                a { +"Helen Troy" }
                                +" added "
                                a { +"2 new illustrations" }
                                noui.date { +"4 days ago" }
                            }
                            noui.meta {
                                noui.like { icon.heart(); +"1 Like" }
                            }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }
}
