package io.peekandpoke.kraft.addons.styling

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLinkElement

/**
 * A stylesheet that links an external CSS file via a `<link>` tag.
 *
 * Auto-mounts by default. Pass [autoMount] = false to defer mounting.
 */
class StyleSheetTag(
    autoMount: Boolean = true,
    block: HTMLLinkElement.() -> Unit,
) : StyleSheetDefinition {

    private val link = (document.createElement("link") as HTMLLinkElement).apply {
        rel = "stylesheet"
        block()
    }

    private val mounter = LinkTagMounter { link }

    init {
        if (autoMount) {
            StyleSheets.mount(this)
        }
    }

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()
}
