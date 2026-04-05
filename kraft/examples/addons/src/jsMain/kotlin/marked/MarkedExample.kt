package io.peekandpoke.kraft.examples.jsaddons.marked

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.addons.marked.MarkedAddon
import io.peekandpoke.kraft.addons.marked.marked
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.jsaddons.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.semanticui.forms.UiTextArea
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.p
import kotlinx.html.unsafe

@Suppress("FunctionName")
fun Tag.MarkedExample() = comp {
    MarkedExample(it)
}

class MarkedExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    // <CodeBlock subscribing>
    private val markedAddon: MarkedAddon? by subscribingTo(addons.marked)
    // </CodeBlock>

    private var input by value(
        """
            # Header 1
            ## Header 2
            ### Header 3

            - item 1
            - item 2

            1. item 1
            2. item 2

            `code`

            <div style="color: red; padding: 20px; border: 1px solid red;" onclick="window.alert('You have been pwnd!')">Evil div with onclick handler.</div>
        """.trimIndent()
    )

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"Markdown rendering (via AddonRegistry)" }

            p {
                +"Uses the AddonRegistry to lazy-load marked.js and DOMPurify."
            }

            p {
                a(href = "https://www.npmjs.com/package/dompurify", target = "_blank") {
                    +"https://www.npmjs.com/package/dompurify"
                }
            }

            p {
                a(href = "https://www.npmjs.com/package/marked", target = "_blank") {
                    +"https://www.npmjs.com/package/marked"
                }
            }

            p {
                a(href = "https://www.markdownguide.org/cheat-sheet/", target = "_blank") {
                    +"Markdown Cheat Sheet"
                }
            }

            ui.dividing.header { +"Subscribing to the addon" }

            HorizontalContentAndCode(
                ExtractedCodeBlocks.marked_MarkedExample_kt_subscribing,
            ) {
                ui.label { +"MarkedAddon is loaded via subscribingTo(addons.marked)" }
            }

            ui.dividing.header { +"Usage" }

            HorizontalContentAndCode(
                ExtractedCodeBlocks.marked_MarkedExample_kt_usage,
            ) {
                ui.form {
                    UiTextArea(input, { input = it })
                }

                val addon = markedAddon

                if (addon != null) {
                    // <CodeBlock usage>
                    ui.segment {
                        unsafe {
                            +addon.markdown2html(input)
                        }
                    }
                    // </CodeBlock>
                } else {
                    ui.placeholder.segment {
                        ui.icon.header {
                            icon.spinner_loading()
                            +"Loading marked addon..."
                        }
                    }
                }
            }
        }
    }
}
