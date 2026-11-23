package io.peekandpoke.kraft.addons.pdfjs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.html.Tag
import kotlinx.html.div
import org.khronos.webgl.Uint8Array
import kotlin.time.Duration.Companion.seconds

/** A one-page PDF without a cross-reference table: pdf.js rebuilds it, in the worker. */
private val onePage = listOf(
    "%PDF-1.1",
    "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj",
    "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj",
    "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 10 10]>>endobj",
    "trailer<</Root 1 0 R>>",
    "%%EOF",
).joinToString("\n")

private fun onePageBytes() = Uint8Array(onePage.length).also { arr ->
    onePage.forEachIndexed { i, c -> arr.asDynamic()[i] = c.code }
}

@Suppress("TestFunctionName")
private fun Tag.ToggledViewer(
    shown: StreamSource<Boolean>,
    scrolling: Boolean,
    onDoc: (PdfjsLib.PDFDocumentProxy) -> Unit,
) =
    comp(ToggledViewer.Props(shown, scrolling, onDoc)) { ToggledViewer(it) }

/** One of the two viewers while [Props.shown] holds true; hands out the document it loaded. */
private class ToggledViewer(ctx: Ctx<Props>) : Component<ToggledViewer.Props>(ctx) {
    data class Props(
        val shown: StreamSource<Boolean>,
        val scrolling: Boolean,
        val onDoc: (PdfjsLib.PDFDocumentProxy) -> Unit,
    )

    private val shown by subscribingTo(props.shown)

    override fun VDom.render() {
        div {
            if (shown) {
                val src = PdfSource.Data(onePageBytes())

                when (props.scrolling) {
                    true -> ScrollingPdfViewer(src, ScrollingPdfViewer.Options()) { it.doc?.let(props.onDoc) }
                    false -> PagedPdfViewer(src, PagedPdfViewer.Options()) { it.doc?.let(props.onDoc) }
                }
            }
        }
    }
}

/** A fake document that counts its destroys. */
private fun fakeDoc(onDestroy: () -> Unit): PdfjsLib.PDFDocumentProxy =
    js("(function (d) { return { destroy: function () { d(); return Promise.resolve(); } }; })")(onDestroy)
        .unsafeCast<PdfjsLib.PDFDocumentProxy>()

/** The viewers destroy their document on unmount, which ends its worker (ULTRA-00349). */
class PdfViewerLifecycleSpec : StringSpec({

    listOf(false to "PagedPdfViewer", true to "ScrollingPdfViewer").forEach { (scrolling, name) ->
        "$name destroys its document when it unmounts" {
            val shown = StreamSource(true)
            var doc: PdfjsLib.PDFDocumentProxy? = null

            TestBed.preact(view = { ToggledViewer(shown, scrolling) { doc = it } }) {
                val loaded = eventually(timeout = 10.seconds) { doc.shouldNotBeNull() }
                loaded.loadingTask.destroyed shouldBe false

                shown(false)
                eventually { loaded.loadingTask.destroyed shouldBe true }
            }
        }
    }

    "the owner adopts the first document, destroys any other and, released, the adopted one and later ones" {
        val destroyed = mutableListOf<String>()
        val first = fakeDoc { destroyed.add("first") }
        val second = fakeDoc { destroyed.add("second") }
        val late = fakeDoc { destroyed.add("late") }
        val owner = PdfDocumentOwner()

        owner.adopt(first) shouldBe true
        owner.adopt(first) shouldBe false
        owner.adopt(second) shouldBe false
        destroyed shouldBe listOf("second")

        owner.release()
        destroyed shouldBe listOf("second", "first")

        owner.adopt(late) shouldBe false
        destroyed shouldBe listOf("second", "first", "late")
    }
})
