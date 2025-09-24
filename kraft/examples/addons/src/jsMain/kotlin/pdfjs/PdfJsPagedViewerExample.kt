package de.peekandpoke.kraft.examples.jsaddons.pdfjs

import de.peekandpoke.kraft.addons.pdfjs.PagedPdfViewer
import de.peekandpoke.kraft.addons.pdfjs.PdfSource
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.ComponentRef
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.css
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.css.Color
import kotlinx.css.Padding
import kotlinx.css.backgroundColor
import kotlinx.css.padding
import kotlinx.css.px
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.PdfJsPagedViewerExample(
    uri: String,
) = comp(
    PdfJsPagedViewerExample.Props(uri = uri)
) {
    PdfJsPagedViewerExample(it)
}

class PdfJsPagedViewerExample(ctx: Ctx<Props>) : Component<PdfJsPagedViewerExample.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val uri: String,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var pdfViewer = ComponentRef.Tracker<PagedPdfViewer>()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.header H2 { +"PDF JS - Paged Pdf Viewer Example" }

        ui.top.attached.center.aligned.segment {

            ui.horizontal.list {
                noui.item {
                    ui.small.icon.button {
                        onClick {
                            pdfViewer {
                                it.gotoPreviousPage()
                            }
                        }
                        icon.minus()
                    }
                }

                noui.middle.aligned.item {
                    pdfViewer {
                        it.getNumPages()?.let { numPages ->
                            +"${it.getCurrentPage()} / $numPages"
                        }
                    }
                }

                noui.item {
                    ui.small.icon.button {
                        onClick {
                            pdfViewer {
                                it.gotoNextPage()
                            }
                        }
                        icon.plus()
                    }
                }

                noui.item {
                    ui.small.icon.button {
                        onClick {
                            pdfViewer {
                                it.modifyScale { scale -> scale - 0.2 }
                            }
                        }
                        icon.search_minus()
                    }
                }

                noui.item {
                    ui.small.icon.button {
                        onClick {
                            pdfViewer {
                                it.modifyScale { scale -> scale + 0.2 }
                            }
                        }
                        icon.search_plus()
                    }
                }
            }
        }

        ui.bottom.attached.segment {
            css {
                padding = Padding(10.px)
                backgroundColor = Color("#F8F8F8")
            }

            PagedPdfViewer(
                src = PdfSource.Url(props.uri),
                options = PagedPdfViewer.Options(
                    maxHeightLandscapeVh = 80,
                    maxHeightPortraitVh = 80,
                )
            ) { triggerRedraw() }.track(pdfViewer)
        }
    }
}
