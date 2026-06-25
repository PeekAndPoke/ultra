package io.peekandpoke.kraft.addons.pdfjs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.khronos.webgl.Uint8Array

/**
 * Pure tests for the [PdfSource] data layer. The CDN-loaded `PdfJs` engine and the canvas-rendering
 * viewers (ScrollingPdfViewer / PagedPdfViewer) need a served PDF fixture + network access, so they
 * are covered by a separate integration test (see the addons testing-strategy plan), not here.
 */
class PdfSourceSpec : StringSpec({

    "PdfSource.Url carries its url" {
        val src: PdfSource = PdfSource.Url("https://example.com/doc.pdf")
        src.shouldBeInstanceOf<PdfSource.Url>()
        src.url shouldBe "https://example.com/doc.pdf"
    }

    "PdfSource.Base64 carries its data" {
        val src: PdfSource = PdfSource.Base64("JVBERi0xLjQK")
        src.shouldBeInstanceOf<PdfSource.Base64>()
        src.data shouldBe "JVBERi0xLjQK"
    }

    "PdfSource.Data carries its bytes" {
        val bytes = Uint8Array(4)
        val src: PdfSource = PdfSource.Data(bytes)
        src.shouldBeInstanceOf<PdfSource.Data>()
        src.data shouldBe bytes
    }

    "PdfSource subtypes compare by value" {
        (PdfSource.Url("a") == PdfSource.Url("a")) shouldBe true
        (PdfSource.Url("a") == PdfSource.Url("b")) shouldBe false
        (PdfSource.Base64("x") == PdfSource.Base64("x")) shouldBe true
    }
})
