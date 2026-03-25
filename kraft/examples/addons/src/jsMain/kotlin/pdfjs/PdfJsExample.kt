package io.peekandpoke.kraft.examples.jsaddons.pdfjs

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.jsaddons.routes
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.PdfJsExample() = comp {
    PdfJsExample(it)
}

class PdfJsExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"PDF JS" }

            val docs = listOf(
                "/pdf/contract.pdf",
                "/pdf/eg14_cats_and_people.pdf",
                "/pdf/Get_Started_With_Smallpdf.pdf",
            )

            ui.basic.segment {
                ui.horizontal.list {
                    noui.item {
                        noui.header { +"Paged" }
                    }
                    docs.forEach { doc ->
                        noui.item A {
                            href(routes.pdfjs.paged(doc))
                            +doc
                        }
                    }
                }
            }

            ui.basic.segment {
                ui.horizontal.list {
                    noui.item {
                        noui.header { +"Scrolling" }
                    }
                    docs.forEach { doc ->
                        noui.item A {
                            href(routes.pdfjs.scrolling(doc))
                            +doc
                        }
                    }
                }
            }
        }
    }
}
