package io.peekandpoke.kraft.addons.pdfjs

import io.peekandpoke.kraft.addons.pdfjs.js.PdfjsLib

/**
 * Owns the documents a viewer loads (ULTRA-00349): each holds a pdf.js worker until it is destroyed.
 *
 * The first document loaded is the viewer's; any other one, and one that arrives after [release], is destroyed at once.
 */
internal class PdfDocumentOwner {

    private var adopted: PdfjsLib.PDFDocumentProxy? = null
    private var released = false

    /** True when [doc] became the viewer's document now; false for the adopted one again and for one destroyed. */
    fun adopt(doc: PdfjsLib.PDFDocumentProxy): Boolean {
        if (doc === adopted) return false

        if (released || adopted != null) {
            doc.destroy()
            return false
        }

        adopted = doc
        return true
    }

    /** Destroys the viewer's document; documents that arrive later are destroyed as they come. */
    fun release() {
        released = true
        adopted?.destroy()
        adopted = null
    }
}
