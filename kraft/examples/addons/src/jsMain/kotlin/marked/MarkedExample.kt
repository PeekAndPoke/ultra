package de.peekandpoke.kraft.examples.jsaddons.marked

import de.peekandpoke.kraft.addons.marked.markdown2html
import de.peekandpoke.kraft.addons.marked.marked
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.utils.jsObject
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
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
            ui.header H2 { +"Markdown rendering" }

            p {
                +"Uses marked.js to render Markdown to HTML and DOMPurify to sanitize the HTML."
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

            ui.form {
                UiTextArea(input, { input = it })
            }

            ui.segment {
                unsafe {
                    marked.use(jsObject {
                        mangle = false
                        headerIds = false
                    })

                    val rendered = markdown2html(input)

                    +rendered
                }
            }
        }
    }
}
