package de.peekandpoke.kraft.addons.styling

import org.w3c.dom.HTMLElement

data class RawStyleSheet(val css: String) : StyleSheetDefinition {
    private var mounter = RawCssMounter { css }

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()
}
