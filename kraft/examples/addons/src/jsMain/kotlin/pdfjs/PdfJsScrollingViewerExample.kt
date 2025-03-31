package de.peekandpoke.kraft.examples.jsaddons.pdfjs

import de.peekandpoke.kraft.addons.pdfjs.PdfSource
import de.peekandpoke.kraft.addons.pdfjs.ScrollingPdfViewer
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.css.*
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.PdfJsScrollingViewerExample(
    uri: String,
) = comp(
    PdfJsScrollingViewerExample.Props(uri = uri)
) {
    PdfJsScrollingViewerExample(it)
}

class PdfJsScrollingViewerExample(ctx: Ctx<Props>) : Component<PdfJsScrollingViewerExample.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val uri: String,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var pdfViewer = ComponentRef.Tracker<ScrollingPdfViewer>()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.header H2 { +"PDF JS - Scrolling Pdf Viewer Example" }

        ui.top.attached.center.aligned.segment {
            ui.horizontal.list {
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

            ScrollingPdfViewer(
                src = PdfSource.Url(props.uri),
                options = ScrollingPdfViewer.Options(
                    maxHeightLandscapeVh = 80,
                    maxHeightPortraitVh = 80,
                )
            ) { triggerRedraw() }.track(pdfViewer)
        }
    }
}
