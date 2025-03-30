@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.loader

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.examples.fomanticui.helpers.shortParagraphWireFrame
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.br
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.LoaderPage() = comp {
    LoaderPage(it)
}

class LoaderPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Loader")

        ui.basic.segment {
            ui.dividing.header H1 { +"Loader" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/loader.html")

            ui.dividing.header H2 { +"Types" }

            renderLoader()
            renderTextLoader()

            ui.dividing.header H2 { +"States" }

            renderIntermediateLoader()
            renderActiveLoader()
            renderDisabledLoader()

            ui.dividing.header H2 { +"Variations" }

            renderInlineLoader()
            renderInlineCenteredLoader()
            renderSpeedLoader()
            renderColorsLoader()
            renderSizedLoader()
            renderInvertedLoader()
            renderStylesLoader()
        }
    }

    private fun FlowContent.renderLoader() = example {
        ui.dividing.header H3 { +"Loader" }

        p { +"A loader" }

        ui.info.message {
            +"Loaders are hidden unless "
            ui.label { +"active" }
            +" or inside an "
            ui.label { +"active dimmer" }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderLoader_1,
        ) {
            // <CodeBlock renderLoader_1>
            ui.basic.segment {
                ui.active.loader()
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderLoader_2,
        ) {
            // <CodeBlock renderLoader_2>
            ui.basic.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.loader()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTextLoader() = example {
        ui.dividing.header H3 { +"Text Loader" }

        p { +"A loader can contain text" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderTextLoader_1,
        ) {
            // <CodeBlock renderTextLoader_1>
            ui.basic.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.text.loader {
                        +"Loading"
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderTextLoader_2,
        ) {
            // <CodeBlock renderTextLoader_2>
            ui.basic.segment {
                shortParagraphWireFrame()
                ui.active.inverted.dimmer {
                    ui.text.loader {
                        +"Loading"
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIntermediateLoader() = example {
        ui.dividing.header H3 { +"Intermediate" }

        p { +"A loader can show it's unsure of how long a task will take" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderIntermediateLoader,
        ) {
            // <CodeBlock renderIntermediateLoader>
            ui.basic.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.indeterminate.text.loader {
                        +"Loading"
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderActiveLoader() = example {
        ui.dividing.header H3 { +"Active" }

        p { +"A loader can be active or visible" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderActiveLoader,
        ) {
            // <CodeBlock renderActiveLoader>
            ui.basic.segment {
                ui.active.loader()
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabledLoader() = example {
        ui.dividing.header H3 { +"Disabled" }

        p { +"A loader can be disabled or hidden" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderDisabledLoader,
        ) {
            // <CodeBlock renderDisabledLoader>
            ui.basic.segment {
                ui.disabled.loader()
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInlineLoader() = example {
        ui.dividing.header H3 { +"Inline" }

        p { +"Loaders can appear inline with content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderInlineLoader,
        ) {
            // <CodeBlock renderInlineLoader>
            ui.active.inline.loader()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInlineCenteredLoader() = example {
        ui.dividing.header H3 { +"Inline centered" }

        p { +"Loaders can appear inline centered with content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderInlineCenteredLoader,
        ) {
            // <CodeBlock renderInlineCenteredLoader>
            ui.active.centered.inline.loader()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSpeedLoader() = example {
        ui.dividing.header H3 { +"Speed" }

        p { +"Loaders can appear slow, normal or fast" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderSpeedLoader,
        ) {
            // <CodeBlock renderSpeedLoader>
            ui.active.slow.inline.loader()
            ui.active.inline.loader()
            ui.active.fast.inline.loader()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColorsLoader() = example {
        ui.dividing.header H3 { +"Colors" }

        p { +"Loaders can be different colors" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderColorsLoader,
        ) {
            // <CodeBlock renderColorsLoader>
            ui.primary.active.inline.loader()
            ui.secondary.active.inline.loader()
            ui.red.active.inline.loader()
            ui.orange.active.inline.loader()
            ui.yellow.active.inline.loader()
            ui.olive.active.inline.loader()
            ui.green.active.inline.loader()
            ui.teal.active.inline.loader()
            ui.blue.active.inline.loader()
            ui.violet.active.inline.loader()
            ui.purple.active.inline.loader()
            ui.pink.active.inline.loader()
            ui.brown.active.inline.loader()
            ui.grey.active.inline.loader()
            ui.black.active.inline.loader()
            ui.white.active.inline.loader()
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSizedLoader() = example {
        ui.dividing.header H3 { +"Size" }

        p { +"Loaders can have different sizes" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderSizedLoader,
        ) {
            // <CodeBlock renderSizedLoader>
            ui.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.mini.loader()
                }
            }
            ui.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.tiny.loader()
                }
            }
            ui.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.small.loader()
                }
            }
            ui.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.loader()
                }
            }
            ui.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.large.loader()
                }
            }
            ui.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.big.loader()
                }
            }
            ui.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.huge.loader()
                }
            }
            ui.segment {
                shortParagraphWireFrame()
                ui.active.dimmer {
                    ui.massive.loader()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInvertedLoader() = example {
        ui.dividing.header H3 { +"Inverted" }

        p { +"Loaders can have their colors inverted." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderInvertedLoader_1,
        ) {
            // <CodeBlock renderInvertedLoader_1>
            ui.inverted.segment {
                ui.active.inverted.loader()
                br()
                br()
                br()
                br()
            }
            // </CodeBlock>
        }

        ui.info.message {
            +"Loaders will automatically be inverted inside "
            ui.label { +"inverted dimmer" }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderInvertedLoader_2,
        ) {
            // <CodeBlock renderInvertedLoader_2>
            ui.segment {
                shortParagraphWireFrame()
                ui.active.inverted.dimmer {
                    ui.loader()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderStylesLoader() = example {
        ui.dividing.header H3 { +"Styles" }

        p {
            +"Loaders can also appear in "
            ui.label { +"double" }
            +" or "
            ui.label { +"elastic" }
            +" animation style. Can be combined with any color or speed."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_loader_LoaderPage_kt_renderStylesLoader,
        ) {
            // <CodeBlock renderStylesLoader>
            ui.segment {
                ui.active.slow.green.double.inverted.loader()
                br()
                br()
                br()
            }

            ui.segment {
                ui.active.blue.elastic.inverted.loader()
                br()
                br()
                br()
            }
            // </CodeBlock>
        }
    }
}
