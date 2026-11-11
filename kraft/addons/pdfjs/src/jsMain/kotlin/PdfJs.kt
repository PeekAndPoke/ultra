package io.peekandpoke.kraft.addons.pdfjs

import io.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.js.Promise

/** Wrapper around the pdf.js library that handles lazy loading and document retrieval. */
class PdfJs private constructor(private val lib: PdfjsLib) {

    companion object {
        private var instance: PdfJs? = null

        /** Returns the singleton [PdfJs] instance, loading the library on first call. */
        suspend fun instance(): PdfJs {
            instance?.let { return it }

            // From the npm package (ULTRA-00306): webpack splits the library into its own chunk, loaded here on first
            // use, and copies the worker into the app's build output; both come from the app's origin, no CDN
            val module: dynamic = (js("import('pdfjs-dist')") as Promise<dynamic>).await()
            // The ES module's namespace, or its default export where a bundler wraps it
            val lib: PdfjsLib = (if (module.GlobalWorkerOptions != null) module else module.default).unsafeCast<PdfjsLib>()

            lib.GlobalWorkerOptions.workerSrc =
                js("new URL('pdfjs-dist/build/pdf.worker.min.mjs', import.meta.url)").href.unsafeCast<String>()

            return PdfJs(lib).also {
                instance = it
            }
        }
    }

    /** Loads a PDF document from a URL [src] and emits the document proxy. */
    fun getDocument(src: String): Flow<PdfjsLib.PDFDocumentProxy> {
        return flow {
            emit(
                lib.getDocument(src).promise.await()
            )
        }
    }

    /** Loads a PDF document from [src] parameters (e.g. binary data) and emits the document proxy. */
    fun getDocument(src: PdfjsLib.GetDocumentParameters): Flow<PdfjsLib.PDFDocumentProxy> {
        return flow {
            emit(
                lib.getDocument(src).promise.await()
            )
        }
    }
}
