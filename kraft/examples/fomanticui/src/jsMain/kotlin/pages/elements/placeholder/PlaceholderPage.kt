@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.placeholder

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
fun Tag.PlaceholderPage() = comp {
    PlaceholderPage(it)
}

class PlaceholderPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Placeholder")

        ui.basic.segment {
            ui.dividing.header H1 { +"Placeholder" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/placeholder.html")

            ui.dividing.header H2 { +"Types" }

            renderPlaceholder()

            ui.dividing.header H2 { +"Content" }

            renderLines()
            renderHeader()
            renderImage()

            ui.dividing.header H2 { +"Variations" }

            renderLineLength()
            renderFluid()
            renderInverted()
        }
    }

    private fun FlowContent.renderPlaceholder() = example {
        ui.dividing.header H3 { +"Placeholder" }

        p { +"A placeholder is used to reserve space for content that soon will appear in a layout." }

        ui.info.message {
            +"Placeholders can includes "
            ui.label { +"paragraph" }
            +", "
            ui.label { +"header" }
            +", "
            ui.label { +"image header" }
            +" and "
            ui.label { +"image" }
            +" to let you format the loaders to emulate the content being loaded."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderPlaceholder_1,
        ) {
            // <CodeBlock renderPlaceholder_1>
            ui.placeholder {
                noui.image.header {
                    noui.line()
                    noui.line()
                }
                noui.paragraph {
                    noui.line()
                    noui.line()
                    noui.line()
                    noui.line()
                    noui.line()
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderPlaceholder_2,
        ) {
            // <CodeBlock renderPlaceholder_2>
            ui.three.column.stackable.grid {
                repeat(3) {
                    noui.column {
                        ui.raised.segment {
                            ui.placeholder {
                                noui.image.header {
                                    noui.line()
                                    noui.line()
                                }
                                noui.paragraph {
                                    noui.line()
                                    noui.line()
                                }
                            }
                        }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderPlaceholder_3,
        ) {
            // <CodeBlock renderPlaceholder_3>
            ui.three.doubling.stackable.cards {
                repeat(3) {
                    noui.card {
                        noui.image {
                            ui.placeholder {
                                noui.square.image()
                            }
                        }
                        noui.content {
                            ui.placeholder {
                                noui.image.header {
                                    noui.line()
                                    noui.line()
                                }
                                noui.paragraph {
                                    noui.line()
                                }
                            }
                        }
                        noui.extra.content {
                            ui.disabled.primary.button { +"Add" }
                            ui.disabled.button { +"Ignore" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLines() = example {
        ui.dividing.header H3 { +"Lines" }

        p { +"A placeholder can contain have lines of text" }

        ui.info.message {
            +"By default, repeated lines will appear varied in width. However, it may be useful to specify "
            +"an exact length to make it match up with content more effectively"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderLines,
        ) {
            // <CodeBlock renderLines>
            ui.placeholder {
                noui.line()
                noui.line()
                noui.line()
                noui.line()
                noui.line()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHeader() = example {
        ui.dividing.header H3 { +"Header" }

        p { +"A placeholder can contain a header" }

        ui.info.message {
            +"Header content will have a slightly larger block size from paragraph"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderHeader,
        ) {
            // <CodeBlock renderHeader>
            ui.placeholder {
                noui.image.header {
                    noui.line()
                    noui.line()
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderHeader_2,
        ) {
            // <CodeBlock renderHeader_2>
            ui.placeholder {
                noui.header {
                    noui.line()
                    noui.line()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderImage() = example {
        ui.dividing.header H3 { +"Image" }

        p { +"A placeholder can contain an image" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderImage_1,
        ) {
            // <CodeBlock renderImage_1>
            ui.placeholder {
                noui.image()
            }
            // </CodeBlock>
        }

        ui.info.message {
            +"Using "
            ui.label { +"square" }
            +" (1:1) or "
            ui.label { +"rectangular" }
            +" (4:3) will embed an aspect ratio into the image loader so that they modify size correctly"
            +" with responsive styles. "
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderImage_2,
        ) {
            // <CodeBlock renderImage_2>
            ui.three.cards {
                repeat(3) {
                    noui.card {
                        noui.content {
                            ui.placeholder {
                                noui.square.image()
                            }
                        }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderImage_3,
        ) {
            // <CodeBlock renderImage_3>
            ui.three.cards {
                repeat(3) {
                    noui.card {
                        noui.content {
                            ui.placeholder {
                                noui.rectangular.image()
                            }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLineLength() = example {
        ui.dividing.header H3 { +"Header" }

        p { +"A line can specify how long its contents should appear" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderLineLength,
        ) {
            // <CodeBlock renderLineLength>
            ui.placeholder {
                noui.full.line()
                noui.very.long.line()
                noui.long.line()
                noui.medium.line()
                noui.short.line()
                noui.very.short.line()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFluid() = example {
        ui.dividing.header H3 { +"Fluid" }

        p { +"A fluid placeholder takes up the width of its container" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderFluid,
        ) {
            // <CodeBlock renderFluid>
            ui.fluid.placeholder {
                noui.image.header {
                    noui.line()
                    noui.line()
                }
                noui.paragraph {
                    noui.line()
                    noui.line()
                    noui.line()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.dividing.header H3 { +"Inverted" }

        p { +"A placeholder can have their colors inverted." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_placeholder_PlaceholderPage_kt_renderInverted,
        ) {
            // <CodeBlock renderInverted>
            ui.inverted.segment {
                ui.inverted.placeholder {
                    noui.image.header {
                        noui.line()
                        noui.line()
                    }
                    noui.paragraph {
                        noui.line()
                        noui.line()
                        noui.line()
                    }
                }
            }
            // </CodeBlock>
        }
    }
}
