@file:Suppress("DuplicatedCode")

package de.peekandpoke.kraft.examples.fomanticui.pages.views.statistic

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.fixture.LoremIpsum
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.br
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.StatisticPage() = comp {
    StatisticPage(it)
}

class StatisticPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Views | Statistic")

        ui.basic.segment {
            ui.dividing.header H1 { +"Statistic" }

            readTheFomanticUiDocs("https://fomantic-ui.com/views/statistic.html")

            ui.dividing.header H2 { +"Types" }

            renderStatistic()
            renderStatisticGroup()

            ui.dividing.header H2 { +"Content" }

            renderContentValue()

            ui.dividing.header H2 { +"Variations" }

            renderVariationsHorizontal()
            renderVariationsColored()
            renderVariationsInverted()
            renderVariationsStackable()
            renderVariationsEvenlyDivided()
            renderVariationsFloated()
            renderVariationsSize()
        }
    }

    private fun FlowContent.renderStatistic() = example {
        ui.header { +"Statistic" }

        p { +"A statistic can display a value with a label above or below it." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderStatistic_1,
        ) {
            // <CodeBlock renderStatistic_1>
            ui.statistic {
                noui.value { +"5,550" }
                noui.label { +"Downloads" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderStatistic_2,
        ) {
            // <CodeBlock renderStatistic_2>
            ui.statistic {
                noui.label { +"Views" }
                noui.value { +"40,550" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderStatisticGroup() = example {
        ui.header { +"Statistic Group" }

        p { +"A group of statistics." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderStatisticGroup,
        ) {
            // <CodeBlock renderStatisticGroup>
            ui.statistics {
                ui.statistic {
                    noui.value { +"5,550" }
                    noui.label { +"Downloads" }
                }
                ui.statistic {
                    noui.value { +"45,550" }
                    noui.label { +"Views" }
                }
                ui.statistic {
                    noui.value { +"22" }
                    noui.label { +"Members" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderContentValue() = example {
        ui.header { +"Value" }

        p { +"A statistic can contain a numeric, icon, image, or text value.'" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderContentValue,
        ) {
            // <CodeBlock renderContentValue>
            ui.statistics {
                ui.statistic {
                    noui.value { +"22" }
                    noui.label { +"Users" }
                }
                ui.statistic {
                    noui.text.value { +"Three"; br(); +"Thousand" }
                    noui.label { +"Signups" }
                }
                ui.statistic {
                    noui.value { icon.plane(); +"5" }
                    noui.label { +"Flights" }
                }
                ui.statistic {
                    noui.value {
                        ui.circular.inline.image Img { src = "images/avatar2/large/kristy.png" }
                        +"42"
                    }
                    noui.label { +"Members" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsHorizontal() = example {
        ui.header { +"Horizontal Statistic" }

        p { +"A statistic can present its measurement horizontally" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsHorizontal_1,
        ) {
            // <CodeBlock renderVariationsHorizontal_1>
            ui.horizontal.statistic {
                noui.value { +"5,550" }
                noui.label { +"Downloads" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsHorizontal_2,
        ) {
            // <CodeBlock renderVariationsHorizontal_2>
            ui.horizontal.statistics {
                ui.statistic {
                    noui.value { +"5,550" }
                    noui.label { +"Downloads" }
                }
                ui.statistic {
                    noui.value { +"45,550" }
                    noui.label { +"Views" }
                }
                ui.statistic {
                    noui.value { +"22" }
                    noui.label { +"Members" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsColored() = example {
        ui.header { +"Colored" }

        p { +"A statistic can be formatted to be different colors" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsColored,
        ) {
            // <CodeBlock renderVariationsColored>
            ui.statistics {
                noui.red.statistic {
                    noui.value { +"21" }
                    noui.label { +"red" }
                }
                noui.orange.statistic {
                    noui.value { +"22" }
                    noui.label { +"orange" }
                }
                noui.yellow.statistic {
                    noui.value { +"23" }
                    noui.label { +"yellow" }
                }
                noui.olive.statistic {
                    noui.value { +"24" }
                    noui.label { +"olive" }
                }
                noui.green.statistic {
                    noui.value { +"25" }
                    noui.label { +"green" }
                }
                noui.teal.statistic {
                    noui.value { +"26" }
                    noui.label { +"teal" }
                }
                noui.blue.statistic {
                    noui.value { +"27" }
                    noui.label { +"blue" }
                }
                noui.violet.statistic {
                    noui.value { +"28" }
                    noui.label { +"violet" }
                }
                noui.purple.statistic {
                    noui.value { +"29" }
                    noui.label { +"purple" }
                }
                noui.pink.statistic {
                    noui.value { +"30" }
                    noui.label { +"pink" }
                }
                noui.brown.statistic {
                    noui.value { +"31" }
                    noui.label { +"brown" }
                }
                noui.grey.statistic {
                    noui.value { +"32" }
                    noui.label { +"grey" }
                }
                noui.black.statistic {
                    noui.value { +"33" }
                    noui.label { +"black" }
                }
                noui.white.statistic {
                    noui.value { +"34" }
                    noui.label { +"white" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsInverted() = example {
        ui.header { +"Inverted" }

        p { +"A statistic can be formatted to fit on a dark background" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsInverted,
        ) {
            // <CodeBlock renderVariationsInverted>
            ui.inverted.segment {
                ui.inverted.statistics {
                    noui.red.statistic {
                        noui.value { +"21" }
                        noui.label { +"red" }
                    }
                    noui.orange.statistic {
                        noui.value { +"22" }
                        noui.label { +"orange" }
                    }
                    noui.yellow.statistic {
                        noui.value { +"23" }
                        noui.label { +"yellow" }
                    }
                    noui.olive.statistic {
                        noui.value { +"24" }
                        noui.label { +"olive" }
                    }
                    noui.green.statistic {
                        noui.value { +"25" }
                        noui.label { +"green" }
                    }
                    noui.teal.statistic {
                        noui.value { +"26" }
                        noui.label { +"teal" }
                    }
                    noui.blue.statistic {
                        noui.value { +"27" }
                        noui.label { +"blue" }
                    }
                    noui.violet.statistic {
                        noui.value { +"28" }
                        noui.label { +"violet" }
                    }
                    noui.purple.statistic {
                        noui.value { +"29" }
                        noui.label { +"purple" }
                    }
                    noui.pink.statistic {
                        noui.value { +"30" }
                        noui.label { +"pink" }
                    }
                    noui.brown.statistic {
                        noui.value { +"31" }
                        noui.label { +"brown" }
                    }
                    noui.grey.statistic {
                        noui.value { +"32" }
                        noui.label { +"grey" }
                    }
                    noui.black.statistic {
                        noui.value { +"33" }
                        noui.label { +"black" }
                    }
                    noui.white.statistic {
                        noui.value { +"34" }
                        noui.label { +"white" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsStackable() = example {
        ui.header { +"Stackable" }

        p { +"A statistic can automatically stack rows to a single columns on mobile devices" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsStackable,
        ) {
            // <CodeBlock renderVariationsStackable>
            ui.stackable.statistics {
                noui.red.statistic {
                    noui.value { +"21" }
                    noui.label { +"red" }
                }
                noui.orange.statistic {
                    noui.value { +"22" }
                    noui.label { +"orange" }
                }
                noui.yellow.statistic {
                    noui.value { +"23" }
                    noui.label { +"yellow" }
                }
                noui.olive.statistic {
                    noui.value { +"24" }
                    noui.label { +"olive" }
                }
                noui.green.statistic {
                    noui.value { +"25" }
                    noui.label { +"green" }
                }
                noui.teal.statistic {
                    noui.value { +"26" }
                    noui.label { +"teal" }
                }
                noui.blue.statistic {
                    noui.value { +"27" }
                    noui.label { +"blue" }
                }
                noui.violet.statistic {
                    noui.value { +"28" }
                    noui.label { +"violet" }
                }
                noui.purple.statistic {
                    noui.value { +"29" }
                    noui.label { +"purple" }
                }
                noui.pink.statistic {
                    noui.value { +"30" }
                    noui.label { +"pink" }
                }
                noui.brown.statistic {
                    noui.value { +"31" }
                    noui.label { +"brown" }
                }
                noui.grey.statistic {
                    noui.value { +"32" }
                    noui.label { +"grey" }
                }
                noui.black.statistic {
                    noui.value { +"33" }
                    noui.label { +"black" }
                }
                noui.white.statistic {
                    noui.value { +"34" }
                    noui.label { +"white" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsEvenlyDivided() = example {
        ui.header { +"Evenly Divided" }

        p { +"A statistic group can have its items divided evenly" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsEvenlyDivided,
        ) {
            // <CodeBlock renderVariationsEvenlyDivided>
            ui.four.statistics {
                ui.statistic {
                    noui.value { +"22" }
                    noui.label { +"Users" }
                }
                ui.statistic {
                    noui.text.value { +"Three"; br(); +"Thousand" }
                    noui.label { +"Signups" }
                }
                ui.statistic {
                    noui.value { icon.plane(); +"5" }
                    noui.label { +"Flights" }
                }
                ui.statistic {
                    noui.value {
                        ui.circular.inline.image Img { src = "images/avatar2/large/kristy.png" }
                        +"42"
                    }
                    noui.label { +"Members" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsFloated() = example {
        ui.header { +"Evenly Divided" }

        p { +"A statistic group can have its items divided evenly" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsFloated,
        ) {
            // <CodeBlock renderVariationsFloated>
            ui.segment {
                ui.right.floated.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                p { +LoremIpsum(50) }
                p { +LoremIpsum(50) }
                ui.left.floated.statistic {
                    noui.value { +"50,220" }
                    noui.label { +"Views" }
                }
                p { +LoremIpsum(50) }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVariationsSize() = example {
        ui.header { +"Size" }

        p { +"A statistic can vary in size" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsSize_1,
        ) {
            // <CodeBlock renderVariationsSize_1>
            ui.statistics {
                ui.mini.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.tiny.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.small.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.medium.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.large.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.big.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.huge.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.massive.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsSize_2,
        ) {
            // <CodeBlock renderVariationsSize_2>
            ui.statistics {
                ui.mini.horizontal.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.tiny.horizontal.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.small.horizontal.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.medium.horizontal.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.large.horizontal.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.big.horizontal.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.huge.horizontal.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.massive.horizontal.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_statistic_StatisticPage_kt_renderVariationsSize_3,
        ) {
            // <CodeBlock renderVariationsSize_3>
            ui.massive.statistics {
                ui.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
                ui.statistic {
                    noui.value { +"10,220" }
                    noui.label { +"Users" }
                }
            }
            // </CodeBlock>
        }
    }
}
