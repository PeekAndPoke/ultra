@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.elements.rail

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.p
import kotlinx.html.style

@Suppress("FunctionName")
fun Tag.RailPage() = comp {
    RailPage(it)
}

class RailPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Rail")

        ui.basic.segment {
            ui.dividing.header H1 { +"Rail" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/rail.html")

            ui.dividing.header H2 { +"Types" }

            renderRail()
            renderInternalRail()
            renderDividingRail()

            ui.dividing.header H2 { +"Variations" }

            renderAttached()
            renderCloseRail()
            renderSize()
        }
    }

    /**
     * Wraps rail examples in a narrow centered container so that left and right rails
     * have enough space and don't overlap the sidebar menu.
     */
    private fun FlowContent.railExample(block: FlowContent.() -> Unit) {
        div {
            style = "max-width: 50vw; margin: 0 auto; position: relative;"
            block()
        }
    }

    private fun FlowContent.renderRail() = example {
        ui.header H3 { +"Rail" }

        p { +"A rail can be presented on the left or right side of a container." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_rail_RailPage_kt_renderRail,
        ) {
            // <CodeBlock renderRail>
            railExample {
                ui.segment {
                    ui.left.rail {
                        ui.segment {
                            +"Left Rail Content"
                        }
                    }
                    ui.right.rail {
                        ui.segment {
                            +"Right Rail Content"
                        }
                    }
                    p { +"Main content area" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInternalRail() = example {
        ui.header H3 { +"Internal" }

        p { +"A rail can attach itself to the inside of a container." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_rail_RailPage_kt_renderInternalRail,
        ) {
            // <CodeBlock renderInternalRail>
            railExample {
                ui.segment {
                    ui.left.internal.rail {
                        ui.segment {
                            +"Left Internal Rail"
                        }
                    }
                    ui.right.internal.rail {
                        ui.segment {
                            +"Right Internal Rail"
                        }
                    }
                    p { +"Main content area" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDividingRail() = example {
        ui.header H3 { +"Dividing" }

        p { +"A rail can create a division between itself and a container." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_rail_RailPage_kt_renderDividingRail,
        ) {
            // <CodeBlock renderDividingRail>
            railExample {
                ui.segment {
                    ui.left.dividing.rail {
                        ui.segment {
                            +"Left Dividing Rail"
                        }
                    }
                    ui.right.dividing.rail {
                        ui.segment {
                            +"Right Dividing Rail"
                        }
                    }
                    p { +"Main content area" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAttached() = example {
        ui.header H3 { +"Attached" }

        p { +"A rail can appear attached to the main viewport." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_rail_RailPage_kt_renderAttached,
        ) {
            // <CodeBlock renderAttached>
            railExample {
                ui.segment {
                    ui.left.attached.rail {
                        ui.segment {
                            +"Left Attached Rail"
                        }
                    }
                    ui.right.attached.rail {
                        ui.segment {
                            +"Right Attached Rail"
                        }
                    }
                    p { +"Main content area" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCloseRail() = example {
        ui.header H3 { +"Close" }

        p { +"A rail can appear closer to the main viewport." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_rail_RailPage_kt_renderCloseRail_1,
        ) {
            // <CodeBlock renderCloseRail_1>
            railExample {
                ui.segment {
                    ui.left.close.rail {
                        ui.segment {
                            +"Left Close Rail"
                        }
                    }
                    ui.right.close.rail {
                        ui.segment {
                            +"Right Close Rail"
                        }
                    }
                    p { +"Main content area" }
                }
            }
            // </CodeBlock>
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_rail_RailPage_kt_renderCloseRail_2,
        ) {
            // <CodeBlock renderCloseRail_2>
            railExample {
                ui.segment {
                    ui.left.very.close.rail {
                        ui.segment {
                            +"Left Very Close Rail"
                        }
                    }
                    ui.right.very.close.rail {
                        ui.segment {
                            +"Right Very Close Rail"
                        }
                    }
                    p { +"Main content area" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"A rail can have different sizes." }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_rail_RailPage_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            railExample {
                ui.segment {
                    ui.left.mini.rail {
                        noui.segment { +"Mini" }
                    }
                    p { +"Main content" }
                }

                ui.segment {
                    ui.left.tiny.rail {
                        noui.segment { +"Tiny" }
                    }
                    p { +"Main content" }
                }

                ui.segment {
                    ui.left.small.rail {
                        noui.segment { +"Small" }
                    }
                    p { +"Main content" }
                }

                ui.segment {
                    ui.left.large.rail {
                        noui.segment { +"Large" }
                    }
                    p { +"Main content" }
                }

                ui.segment {
                    ui.left.big.rail {
                        noui.segment { +"Big" }
                    }
                    p { +"Main content" }
                }

                ui.segment {
                    ui.left.huge.rail {
                        noui.segment { +"Huge" }
                    }
                    p { +"Main content" }
                }

                ui.segment {
                    ui.left.massive.rail {
                        noui.segment { +"Massive" }
                    }
                    p { +"Main content" }
                }
            }
            // </CodeBlock>
        }
    }
}
