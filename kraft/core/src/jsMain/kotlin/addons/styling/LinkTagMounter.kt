package io.peekandpoke.kraft.addons.styling

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLinkElement

internal class LinkTagMounter(private val link: () -> HTMLLinkElement) {

    var mounted: HTMLLinkElement? = null

    fun mount(into: HTMLElement) {
        if (mounted == null) {
            mounted = link()
            into.appendChild(mounted!!)
        }
    }

    fun unmount() {
        mounted?.let {
            it.remove()
            mounted = null
        }
    }
}
