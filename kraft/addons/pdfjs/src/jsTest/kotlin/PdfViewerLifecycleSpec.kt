package io.peekandpoke.kraft.addons.pdfjs

import io.kotest.assertions.throwables.shouldThrowAny
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
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.first
import kotlinx.html.Tag
import kotlinx.html.div
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise
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

/** Counts the destroys of pdf.js workers: destroying a document destroys its worker. */
private class WorkerDestroys private constructor(private val proto: dynamic, private val original: dynamic) {
    var count = 0

    fun uninstall() {
        proto.destroy = original
    }

    companion object {
        suspend fun install(): WorkerDestroys {
            PdfJs.instance()
            val module: dynamic = (js("import('pdfjs-dist')") as Promise<dynamic>).await()
            val lib: dynamic = if (module.GlobalWorkerOptions != null) module else module.default
            val proto: dynamic = lib.PDFWorker.prototype
            val counter = WorkerDestroys(proto, proto.destroy)
            val onDestroy = { counter.count++; Unit }

            proto.destroy = js(
                "(function (original, onDestroy) { " +
                    "return function () { onDestroy(); return original.apply(this, arguments); }; })"
            )(
                counter.original,
                onDestroy,
            )

            return counter
        }
    }
}

/** A fake document that counts its destroys. */
private fun fakeDoc(onDestroy: () -> Unit): PdfjsLib.PDFDocumentProxy =
    js("(function (d) { return { destroy: function () { d(); return Promise.resolve(); } }; })")(onDestroy)
        .unsafeCast<PdfjsLib.PDFDocumentProxy>()

/** The viewers destroy their document on unmount, also one that arrives later, which ends its worker (ULTRA-00349). */
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

    "a viewer unmounted before its document arrives still ends the worker" {
        val workers = WorkerDestroys.install()

        try {
            listOf(false, true).forEach { scrolling ->
                val shown = StreamSource(true)
                val before = workers.count
                var reached = false

                TestBed.preact(view = { ToggledViewer(shown, scrolling) { reached = true } }) {
                    // Right after the first frame: the document comes from the worker later still. Should it reach
                    // the viewer first, this is the ordinary unmount, and `reached` says so below
                    shown(false)
                    eventually(timeout = 10.seconds) { workers.count shouldBe before + 1 }
                    reached shouldBe false
                }
            }
        } finally {
            workers.uninstall()
        }
    }

    "a load that fails ends its worker" {
        val workers = WorkerDestroys.install()

        try {
            val notAPdf = Uint8Array(8)
            val params = js("({})").unsafeCast<PdfjsLib.GetDocumentParameters>().apply { data = notAPdf }

            shouldThrowAny { PdfJs.instance().getDocument(params).first() }
            eventually(timeout = 10.seconds) { workers.count shouldBe 1 }
        } finally {
            workers.uninstall()
        }
    }

    "the owner keeps the first document, destroys any other and, released, the kept one and later ones" {
        val destroyed = mutableListOf<String>()
        val first = fakeDoc { destroyed.add("first") }
        val second = fakeDoc { destroyed.add("second") }
        val late = fakeDoc { destroyed.add("late") }
        val owner = PdfDocumentOwner()

        owner.offer(first)
        owner.offer(first)
        owner.offer(second)
        destroyed shouldBe listOf("second")

        owner.claim(second) shouldBe false
        owner.claim(first) shouldBe true
        owner.claim(first) shouldBe false

        owner.release()
        destroyed shouldBe listOf("second", "first")
        owner.isReleased shouldBe true

        owner.offer(late)
        destroyed shouldBe listOf("second", "first", "late")
    }

    "a document offered after release, before any claim, is destroyed and cannot be claimed" {
        val destroyed = mutableListOf<String>()
        val doc = fakeDoc { destroyed.add("doc") }
        val owner = PdfDocumentOwner()

        owner.release()
        owner.offer(doc)

        destroyed shouldBe listOf("doc")
        owner.claim(doc) shouldBe false
    }
})
