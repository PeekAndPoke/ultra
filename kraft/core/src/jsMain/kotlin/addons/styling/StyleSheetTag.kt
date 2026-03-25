package io.peekandpoke.kraft.addons.styling

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLinkElement

class StyleSheetTag(
    block: HTMLLinkElement.() -> Unit,
) : StyleSheetDefinition {

    private val link = (document.createElement("link") as HTMLLinkElement).apply {
        rel = "stylesheet"
        block()
    }

    private val mounter = LinkTagMounter { link }

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()
}
