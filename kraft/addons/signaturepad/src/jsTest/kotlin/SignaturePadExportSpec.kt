package io.peekandpoke.kraft.addons.signaturepad

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

/**
 * Pure tests for [SignaturePad.Export] — exercises the canvas → data-URL export without needing the
 * full component, addon, or a drawn stroke (an empty canvas still exports valid raster images).
 */
class SignaturePadExportSpec : StringSpec({

    fun canvas(): HTMLCanvasElement {
        val c = document.createElement("canvas") as HTMLCanvasElement
        c.width = 100
        c.height = 50
        return c
    }

    "Export.toPng returns a PNG data URL" {
        val png = SignaturePad.Export(lazy { canvas() }).toPng()
        png shouldNotBe null
        png!!.asDataUrl().startsWith("data:image/png") shouldBe true
    }

    "Export.toJpg returns a JPEG data URL" {
        val jpg = SignaturePad.Export(lazy { canvas() }).toJpg()
        jpg shouldNotBe null
        jpg!!.asDataUrl().startsWith("data:image/jpeg") shouldBe true
    }

    "Export with no canvas returns null" {
        SignaturePad.Export(lazy<HTMLCanvasElement?> { null }).toPng() shouldBe null
    }
})
