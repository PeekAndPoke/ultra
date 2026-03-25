package io.peekandpoke.kraft.examples.jsaddons.pdfjs

import io.peekandpoke.kraft.addons.pdfjs.PagedPdfViewer
import io.peekandpoke.kraft.addons.pdfjs.PdfSource
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.ComponentRef
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
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
