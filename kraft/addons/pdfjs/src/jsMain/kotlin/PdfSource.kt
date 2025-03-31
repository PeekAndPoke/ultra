package de.peekandpoke.kraft.addons.pdfjs

import de.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib
import de.peekandpoke.kraft.utils.jsObject
import de.peekandpoke.ultra.common.atob
import kotlinx.coroutines.flow.Flow
import org.khronos.webgl.Uint8Array

sealed class PdfSource {
    data class Url(val url: String) : PdfSource()
    data class Base64(val data: String) : PdfSource()
    data class Data(val data: Uint8Array) : PdfSource()
}

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
