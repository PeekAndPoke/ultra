@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.views.advertisement

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.AdvertisementPage() = comp {
    AdvertisementPage(it)
}

class AdvertisementPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Views | Advertisement")

        ui.basic.segment {
            ui.dividing.header H1 { +"Advertisement" }

            readTheFomanticUiDocs("https://fomantic-ui.com/views/advertisement.html")

            ui.info.message {
                +"An advertisement element is a wrapper for third-party ad network content like Google AdSense. "
                +"The examples below use the 'test' variation to make ad placements visible."
            }

            ui.dividing.header H2 { +"Types" }

            renderCommonUnits()
            renderMobile()
            renderRectangle()
            renderButton()
            renderSkyscraper()
            renderBanner()
            renderLeaderboard()

            ui.dividing.header H2 { +"Variations" }

            renderCentered()
            renderTest()
        }
    }

    private fun FlowContent.renderCommonUnits() = example {
        ui.header H3 { +"Common Units" }

        p { +"An ad can appear in common ad unit sizes." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderCommonUnits,
        ) {
            // <CodeBlock renderCommonUnits>
            ui.medium.rectangle.test.ad {
                attributes["data-text"] = "Medium Rectangle"
            }

            ui.divider {}

            ui.large.rectangle.test.ad {
                attributes["data-text"] = "Large Rectangle"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderMobile() = example {
        ui.header H3 { +"Mobile" }

        p { +"An ad can use common mobile ad sizes." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderMobile,
        ) {
            // <CodeBlock renderMobile>
            ui.mobile.banner.test.ad {
                attributes["data-text"] = "Mobile Banner"
            }

            ui.divider {}

            ui.mobile.leaderboard.test.ad {
                attributes["data-text"] = "Mobile Leaderboard"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRectangle() = example {
        ui.header H3 { +"Rectangle" }

        p { +"An ad can use a common rectangle ad size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderRectangle,
        ) {
            // <CodeBlock renderRectangle>
            ui.rectangle.test.ad {
                attributes["data-text"] = "Rectangle"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderButton() = example {
        ui.header H3 { +"Button" }

        p { +"An ad can use button ad unit size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderButton,
        ) {
            // <CodeBlock renderButton>
            ui.button.test.ad {
                attributes["data-text"] = "Button"
            }

            ui.divider {}

            ui.square.button.test.ad {
                attributes["data-text"] = "Square Button"
            }

            ui.divider {}

            ui.small.button.test.ad {
                attributes["data-text"] = "Small Button"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSkyscraper() = example {
        ui.header H3 { +"Skyscraper" }

        p { +"An ad can use skyscraper ad unit size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderSkyscraper,
        ) {
            // <CodeBlock renderSkyscraper>
            ui.skyscraper.test.ad {
                attributes["data-text"] = "Skyscraper"
            }

            ui.divider {}

            ui.wide.skyscraper.test.ad {
                attributes["data-text"] = "Wide Skyscraper"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderBanner() = example {
        ui.header H3 { +"Banner" }

        p { +"An ad can use banner ad unit size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderBanner,
        ) {
            // <CodeBlock renderBanner>
            ui.banner.test.ad {
                attributes["data-text"] = "Banner"
            }

            ui.divider {}

            ui.half.banner.test.ad {
                attributes["data-text"] = "Half Banner"
            }

            ui.divider {}

            ui.top.banner.test.ad {
                attributes["data-text"] = "Top Banner"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLeaderboard() = example {
        ui.header H3 { +"Leaderboard" }

        p { +"An ad can use leaderboard ad unit size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderLeaderboard,
        ) {
            // <CodeBlock renderLeaderboard>
            ui.leaderboard.test.ad {
                attributes["data-text"] = "Leaderboard"
            }

            ui.divider {}

            ui.large.leaderboard.test.ad {
                attributes["data-text"] = "Large Leaderboard"
            }

            ui.divider {}

            ui.billboard.test.ad {
                attributes["data-text"] = "Billboard"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCentered() = example {
        ui.header H3 { +"Centered" }

        p { +"An ad can appear centered." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderCentered,
        ) {
            // <CodeBlock renderCentered>
            ui.centered.banner.test.ad {
                attributes["data-text"] = "Centered Banner"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTest() = example {
        ui.header H3 { +"Test" }

        p {
            +"A 'test' ad can be formatted to help verify placement. "
            +"You can adjust the text via the data-text attribute."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_advertisement_AdvertisementPage_kt_renderTest,
        ) {
            // <CodeBlock renderTest>
            ui.medium.rectangle.test.ad {
                attributes["data-text"] = "This is a test ad"
            }
            // </CodeBlock>
        }
    }
}
