package io.peekandpoke.kraft.addons.pdfjs

import io.peekandpoke.kraft.addons.pdfjs.PdfUtils.visitGuarded
import io.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.DataLoader
import io.peekandpoke.kraft.utils.SimpleAsyncQueue
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.css.Overflow
import kotlinx.css.TextAlign
import kotlinx.css.height
import kotlinx.css.overflow
import kotlinx.css.pct
import kotlinx.css.textAlign
import kotlinx.css.width
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.div
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

/** Renders a single page of a PDF at a time with page navigation. */
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

/** Kraft component that renders one PDF page at a time with navigation and zoom controls. */
class PagedPdfViewer(ctx: Ctx<Props>) : Component<PagedPdfViewer.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val src: PdfSource,
        val options: Options,
        val onChange: (State) -> Unit,
    )

    /** Display options for the paged PDF viewer. */
    data class Options(
        val maxHeightLandscapeVh: Int = 80,
        val maxHeightPortraitVh: Int = 80,
        val scaleRange: ClosedFloatingPointRange<Double> = 0.1..3.0,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    /** Observable state of the paged PDF viewer. */
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

    /** Returns the current viewer state. */
    fun getState(): State = state

    /** Returns the currently displayed page number. */
    fun getCurrentPage(): Int = state.page

    /** Returns the total number of pages, or null if the document is not yet loaded. */
    fun getNumPages(): Int? = state.doc?.numPages

    /** Adjusts the zoom scale by applying [block] to the current scale, clamped to [Options.scaleRange]. */
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

    /** Navigates to the previous page, clamped to page 1. */
    fun gotoPreviousPage() {
        modifyState {
            copy(
                page = maxOf(1, page - 1)
            )
        }
    }

    /** Navigates to the next page, clamped to the last page. */
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
