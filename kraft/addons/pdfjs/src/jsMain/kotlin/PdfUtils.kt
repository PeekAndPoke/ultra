package io.peekandpoke.kraft.addons.pdfjs

import org.w3c.dom.HTMLCanvasElement

internal object PdfUtils {

    private val lockedCanvases = mutableSetOf<HTMLCanvasElement>()

    /**
     * PdfJs cannot draw to the same canvas in parallel
     *
     * So we need to guard each canvas, from being accessed multiple times in parallel. An error is logged unless
     * [quiet] says so: a render that the viewer's unmount cut off fails, and that is no news.
     */
    inline fun HTMLCanvasElement.visitGuarded(
        quiet: () -> Boolean = { false },
        block: (HTMLCanvasElement) -> Unit,
    ) {
        if (lockedCanvases.contains(this)) {
            return
        }

        lockedCanvases.add(this)

        try {
            block(this)
        } catch (e: Throwable) {
            if (!quiet()) console.error(e)
        }

        lockedCanvases.remove(this)
    }
}
