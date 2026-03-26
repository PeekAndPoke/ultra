package io.peekandpoke.kraft.addons.pdfjs

import io.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.ultra.common.atob
import kotlinx.coroutines.flow.Flow
import org.khronos.webgl.Uint8Array

/** Represents the source of a PDF document -- by URL, Base64 string, or raw binary data. */
sealed class PdfSource {
    /** Load a PDF from a remote [url]. */
    data class Url(val url: String) : PdfSource()

    /** Load a PDF from a Base64-encoded [data] string. */
    data class Base64(val data: String) : PdfSource()

    /** Load a PDF from raw [data] bytes. */
    data class Data(val data: Uint8Array) : PdfSource()
}

/** Loads this [PdfSource] into a PDF document proxy via pdf.js. */
suspend fun PdfSource.load(): Flow<PdfjsLib.PDFDocumentProxy> {
    return when (val src = this) {
        is PdfSource.Url -> PdfJs.instance().getDocument(src.url)

        is PdfSource.Base64 -> PdfJs.instance().getDocument(jsObject<PdfjsLib.GetDocumentParameters> {
            this.data = atob(src.data)
        })

        is PdfSource.Data -> PdfJs.instance().getDocument(jsObject<PdfjsLib.GetDocumentParameters> {
            this.data = src.data
        })
    }
}
