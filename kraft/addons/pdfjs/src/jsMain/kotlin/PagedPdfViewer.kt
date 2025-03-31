package de.peekandpoke.kraft.addons.pdfjs

import de.peekandpoke.kraft.addons.pdfjs.PdfUtils.visitGuarded
import de.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.utils.DataLoader
import de.peekandpoke.kraft.utils.SimpleAsyncQueue
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.jsObject
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.css.*
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.div
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

@Suppress("FunctionName")
fun Tag.PagedPdfViewer(
    src: PdfSource,
    options: PagedPdfViewer.Options,
    onChange: (PagedPdfViewer.State) -> Unit = {},
) = comp(
    PagedPdfViewer.Props(
        src = src,
        options = options,
        onChange = onChange,
    )
) {
    PagedPdfViewer(it)
}

class PagedPdfViewer(ctx: Ctx<Props>) : Component<PagedPdfViewer.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val src: PdfSource,
        val options: Options,
        val onChange: (State) -> Unit,
    )

    data class Options(
        val maxHeightLandscapeVh: Int = 80,
        val maxHeightPortraitVh: Int = 80,
        val scaleRange: ClosedFloatingPointRange<Double> = 0.1..3.0,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val doc: PdfjsLib.PDFDocumentProxy? = null,
        val page: Int = 1,
        val scale: Double = 1.0,
    )

    private val jobQueue = SimpleAsyncQueue()

    private val pdfLoader = dataLoader { props.src.load() }

    private var state: State by value(State()) { props.onChange(it) }

    private fun modifyState(block: State.() -> State) {
        state = state.block()

        jobQueue.add {
            renderPage()
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        pdfLoader.state {
            val doc = (it as? DataLoader.State.Loaded<PdfjsLib.PDFDocumentProxy>)?.data

            if (doc != null && state.doc == null) {
                jobQueue.add {
                    calculateInitialScale(doc)
                    modifyState { copy(doc = doc) }
                }
            }
        }

        lifecycle {
            onMount {
                dom?.let {
                    val canvas = getCanvas()
                    canvas.width = it.offsetWidth
                    canvas.height = it.offsetHeight
                }
            }
        }
    }

    fun getState(): State = state

    fun getCurrentPage(): Int = state.page

    fun getNumPages(): Int? = state.doc?.numPages

    fun modifyScale(block: (Double) -> Double) {
        modifyState {
            copy(
                scale = maxOf(
                    props.options.scaleRange.start,
                    minOf(
                        block(scale),
                        props.options.scaleRange.endInclusive,
                    )
                )
            )
        }
    }

    fun gotoPreviousPage() {
        modifyState {
            copy(
                page = maxOf(1, page - 1)
            )
        }
    }

    fun gotoNextPage() {
        modifyState {
            copy(
                page = minOf(doc?.numPages ?: 1, page + 1)
            )
        }
    }

    override fun VDom.render() {

        div {
            css {
                width = 100.pct
                height = 100.pct
                textAlign = TextAlign.center
                overflow = Overflow.hidden
            }

            canvas {}
        }
    }

    private suspend fun calculateInitialScale(doc: PdfjsLib.PDFDocumentProxy) {
        val page = doc.getPage(state.page).await()

        val (pageLeft, pageTop, pageRight, pageBottom) = page.view

        val pageWidth = pageRight.toDouble() - pageLeft.toDouble()
        val pageHeight = pageBottom.toDouble() - pageTop.toDouble()

        modifyState {
            copy(
                scale = minOf(
                    dom!!.offsetWidth / pageWidth,
                    calcMaxHeight() / pageHeight,
                )
            )
        }
    }

    private suspend fun renderPage() {
        state.doc?.let { doc ->
            getCanvas().visitGuarded { canvas ->
                val context = canvas.getContext("2d") as CanvasRenderingContext2D

                val page = doc.getPage(state.page).await()

                val viewport = page.getViewport(jsObject {
                    this.scale = state.scale
                })

//            console.log("Got page", page, viewport)

                canvas.width = viewport.width
                canvas.height = viewport.height

                page.render(jsObject {
                    this.canvasContext = context
                    this.viewport = viewport
                }).promise.await()

//            console.log("rendered page")
            }
        }
    }

    private fun calcMaxHeight(): Int {
        return if (window.innerHeight > window.innerWidth) {
            // portrait
            (window.innerHeight * props.options.maxHeightPortraitVh) / 100
        } else {
            // landscape
            (window.innerHeight * props.options.maxHeightLandscapeVh) / 100
        }
    }

    private fun getCanvas(): HTMLCanvasElement {
        return dom!!.querySelector("canvas") as HTMLCanvasElement
    }
}
