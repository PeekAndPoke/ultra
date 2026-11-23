package io.peekandpoke.kraft.addons.pdfjs

import io.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib

/**
 * Owns the documents a viewer loads (ULTRA-00349): each holds a pdf.js worker until it is destroyed.
 *
 * The loader hands it every document ([offer]), also one that arrives after the viewer unmounted: the viewer's own
 * subscriptions are gone by then. The first document is the viewer's; any other one, and one offered after [release],
 * is destroyed at once.
 */
internal class PdfDocumentOwner {

    private var adopted: PdfjsLib.PDFDocumentProxy? = null
    private var claimed = false

    /** True once [release] ran: the viewer is gone, its work can stop quietly. */
    var isReleased = false
        private set

    /** Called by the loader for every document it produces. */
    fun offer(doc: PdfjsLib.PDFDocumentProxy) {
        if (doc === adopted) return

        if (isReleased || adopted != null) {
            destroy(doc)
            return
        }

        adopted = doc
    }

    /** True once, for the adopted document, while the viewer is mounted: the viewer starts showing it. */
    fun claim(doc: PdfjsLib.PDFDocumentProxy): Boolean {
        if (isReleased || claimed || doc !== adopted) return false

        claimed = true
        return true
    }

    /** Destroys the viewer's document; documents offered later are destroyed as they come. */
    fun release() {
        isReleased = true
        adopted?.let { destroy(it) }
        adopted = null
    }

    // A failed destroy has nothing left to clean up: it must not surface as an unhandled rejection
    private fun destroy(doc: PdfjsLib.PDFDocumentProxy) {
        doc.destroy().catch { }
    }
}
