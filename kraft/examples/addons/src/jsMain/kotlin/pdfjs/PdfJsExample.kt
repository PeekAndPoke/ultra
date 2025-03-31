package de.peekandpoke.kraft.examples.jsaddons.pdfjs

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.jsaddons.routes
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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
                "pdf/contract.pdf",
                "pdf/eg14_cats_and_people.pdf",
                "pdf/Get_Started_With_Smallpdf.pdf",
            )

            ui.basic.segment {
                ui.horizontal.list {
                    noui.item {
                        noui.header { +"Paged" }
                    }
                    docs.forEach { doc ->
                        noui.item A {
                            href = routes.pdfjs.paged(doc)
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
                            href = routes.pdfjs.scrolling(doc)
                            +doc
                        }
                    }
                }
            }
        }
    }
}
