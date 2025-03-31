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
import kotlinx.coroutines.delay
import kotlinx.css.*
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.div
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.asList

@Suppress("FunctionName")
fun Tag.ScrollingPdfViewer(
    src: PdfSource,
    options: ScrollingPdfViewer.Options,
    onChange: (ScrollingPdfViewer.State) -> Unit = {},
) = comp(
    ScrollingPdfViewer.Props(
        src = src,
        options = options,
        onChange = onChange,
    )
) {
    ScrollingPdfViewer(it)
}

class ScrollingPdfViewer(ctx: Ctx<Props>) : Component<ScrollingPdfViewer.Props>(ctx) {

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
        val scale: Double = 1.0,
    )

    private val jobQueue = SimpleAsyncQueue()

    private val pdfLoader = dataLoader { props.src.load() }

    private var state: State by value(State()) { props.onChange(it) }

    private fun modifyState(block: State.() -> State) {
        state = state.block()

        jobQueue.add {
            renderAllPages()
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        pdfLoader.state {
            console.log(it)

            val doc = (it as? DataLoader.State.Loaded<PdfjsLib.PDFDocumentProxy>)?.data

            if (doc != null && state.doc == null) {
                modifyState { copy(doc = doc) }

                jobQueue.add {
                    delay(10)
                    renderAllPages()
                }
            }
        }
    }

    fun getState(): State = state

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

    override fun VDom.render() {

        div {
            css {
                width = 100.pct
                height = calcMaxHeight().px
                overflowY = Overflow.auto
                overflowX = Overflow.auto
            }

            state.doc?.numPages?.let { numPages ->
                (1..numPages).forEach { idx ->

                    div("page page-$idx") {
                        css {
                            textAlign = TextAlign.center
                            overflow = Overflow.hidden
                        }
                        canvas {
                            css {
                                overflow = Overflow.hidden
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun renderAllPages() {

        val canvases: List<HTMLCanvasElement> = dom?.querySelectorAll(".page canvas")
            ?.asList()
            ?.filterIsInstance<HTMLCanvasElement>()
            ?: emptyList()

        val doc = state.doc

        // Wait for all canvases to be rendered and the document to be loaded
        if (doc == null || canvases.size < doc.numPages) {
            delay(100)
            renderAllPages()
            return
        }

        canvases.forEachIndexed { idx, canvas ->
            jobQueue.add {
                delay(10)

                try {
                    renderPage(canvas, idx + 1)
                } catch (e: Throwable) {
                    console.log(e)
                }
            }
        }
    }

    private suspend fun renderPage(canvas: HTMLCanvasElement, pageNumber: Int) {

        state.doc?.let { doc: PdfjsLib.PDFDocumentProxy ->
            canvas.visitGuarded { canvas ->
                val context = canvas.getContext("2d") as CanvasRenderingContext2D

                val page = doc.getPage(pageNumber).await()

                val (pageLeft, pageTop, pageRight, pageBottom) = page.view

                val pageWidth = pageRight.toDouble() - pageLeft.toDouble()
                val pageHeight = pageBottom.toDouble() - pageTop.toDouble()

                val pageScale = minOf(
                    (dom!!.offsetWidth * 0.95) / pageWidth,
                    (dom!!.offsetHeight * 0.95) / pageHeight,
                )

                val viewport = page.getViewport(jsObject {
                    this.scale = pageScale * state.scale
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
}
