package de.peekandpoke.kraft.addons.pdfjs

import org.w3c.dom.HTMLCanvasElement

internal object PdfUtils {

    private val lockedCanvases = mutableSetOf<HTMLCanvasElement>()

    /**
     * PdfJs cannot draw to the same canvas in parallel
     *
     * So we need to guard each canvas, from being accessed multiple times in parallel
     */
    inline fun HTMLCanvasElement.visitGuarded(
        block: (HTMLCanvasElement) -> Unit,
    ) {
        if (lockedCanvases.contains(this)) {
            return
        }

        lockedCanvases.add(this)

        try {
            block(this)
        } catch (e: Throwable) {
            console.error(e)
        }

        lockedCanvases.remove(this)
    }
}
