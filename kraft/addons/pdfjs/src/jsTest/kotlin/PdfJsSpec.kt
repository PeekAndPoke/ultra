package io.peekandpoke.kraft.addons.pdfjs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.first
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

/** pdf.js and its worker come from the npm package, served from the app's origin (ULTRA-00306). */
class PdfJsSpec : StringSpec({

    /** A one-page PDF without a cross-reference table: pdf.js rebuilds it, in the worker. */
    val onePage = listOf(
        "%PDF-1.1",
        "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj",
        "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj",
        "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 10 10]>>endobj",
        "trailer<</Root 1 0 R>>",
        "%%EOF",
    ).joinToString("\n")

    "the library loads, its worker is served from the app's origin" {
        PdfJs.instance()

        val module: dynamic = (js("import('pdfjs-dist')") as Promise<dynamic>).await()
        val lib: dynamic = if (module.GlobalWorkerOptions != null) module else module.default

        lib.GlobalWorkerOptions.workerSrc.unsafeCast<String>() shouldStartWith window.location.origin
    }

    "a document is opened through the worker" {
        val bytes = Uint8Array(onePage.length).also { arr ->
            onePage.forEachIndexed { i, c -> arr.asDynamic()[i] = c.code }
        }
        val params = js("({})").unsafeCast<PdfjsLib.GetDocumentParameters>().apply { data = bytes }

        val doc = PdfJs.instance().getDocument(params).first()

        doc.numPages shouldBe 1
    }
})
