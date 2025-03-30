package de.peekandpoke.kraft.addons.pdfjs

import de.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib
import de.peekandpoke.kraft.utils.ScriptLoader
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PdfJs private constructor(private val lib: PdfjsLib) {

    companion object {
        private var instance: PdfJs? = null

        var librarySource: LibrarySrc = LibrarySrc.CdnJs.v4_7_76

        suspend fun instance(): PdfJs {
            instance?.let { return it }

            ScriptLoader.load(librarySource.src).await()

            val winDyn = window.asDynamic()

            val candidates = sequenceOf(
                { "window.pdfjsLib" to winDyn.pdfjsLib },
                { "window.exports" to winDyn.exports },
                { "window.module" to winDyn.module },
                { "window.module?.exports" to winDyn.module?.exports }
            )

            val (name: String, lib: PdfjsLib) = candidates
                .map { it() }
                .first { (_, item) ->
                    @Suppress("UnsafeCastFromDynamic")
                    !!item && !!item.getDocument
                }

            console.info("pdfjsLib was loaded into $name", lib)

            lib.GlobalWorkerOptions.workerSrc = librarySource.workerSrc

            @Suppress("UnsafeCastFromDynamic")
            return PdfJs(lib).also {
                instance = it
            }
        }
    }

    interface LibrarySrc {
        val src: ScriptLoader.Javascript.Default
        val workerSrc: String

        /**
         * https://cdnjs.com/libraries/pdf.js
         */
        data class CdnJs(
            override val src: ScriptLoader.Javascript.Default,
            override val workerSrc: String,
        ) : LibrarySrc {

            companion object {
                @Suppress("unused")
                val v2_16_105 = CdnJs(
                    src = ScriptLoader.Javascript.Default(
                        src = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.min.js",
                        integrity = "sha512-tqaIiFJopq4lTBmFlWF0MNzzTpDsHyug8tJaaY0VkcH5AR2ANMJlcD+3fIL+RQ4JU3K6edt9OoySKfCCyKgkng==",
                    ),
                    workerSrc = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.worker.min.js",
                )

                // https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.7.76/pdf.min.mjs

                val v4_7_76 = CdnJs(
                    src = ScriptLoader.Javascript.Default(
                        src = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.7.76/pdf.min.mjs",
                        type = "module",
                        integrity = "sha384-qgyx6GmMWoI003drRr62DU41/67b3n7M2G0EXu2WhaOsBqONtHyay9Vw4aIivyOX",
                    ),
                    workerSrc = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.7.76/pdf.worker.min.mjs",
                )
            }
        }
    }

    fun getDocument(src: String): Flow<PdfjsLib.PDFDocumentProxy> {
        return flow {
            emit(
                lib.getDocument(src).promise.await()
            )
        }
    }

    fun getDocument(src: PdfjsLib.GetDocumentParameters): Flow<PdfjsLib.PDFDocumentProxy> {
        return flow {
            emit(
                lib.getDocument(src).promise.await()
            )
        }
    }
}
