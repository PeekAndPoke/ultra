package io.peekandpoke.kraft.addons.styling

import org.w3c.dom.HTMLElement

/**
 * A stylesheet that injects a raw CSS string into the page.
 *
 * Auto-mounts by default. Pass [autoMount] = false to defer mounting.
 */
class RawStyleSheet(val css: String, autoMount: Boolean = true) : StyleSheetDefinition {
    private var mounter = RawCssMounter { css }

    init {
        if (autoMount) {
            StyleSheets.mount(this)
        }
    }

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()
}
