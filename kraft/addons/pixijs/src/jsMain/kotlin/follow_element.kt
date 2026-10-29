package io.peekandpoke.kraft.addons.pixijs

import io.peekandpoke.kraft.addons.pixijs.js.Application
import io.peekandpoke.kraft.ext.ResizeObserver
import org.w3c.dom.HTMLElement

/**
 * Keeps the app the size of [element], also when layout alone resizes it (a panel docked under it, a splitter drag),
 * which `resizeTo` does not notice: it follows the window's `resize` event only (ULTRA-00262, from Klang). A
 * `ResizeObserver` asks for [Application.queueResize], so several changes in one frame resize once. Sets `resizeTo` to
 * [element]. Returns the stop; call it before `destroy`.
 */
fun Application.followElementSize(element: HTMLElement): () -> Unit {
    if (asDynamic().resizeTo !== element) {
        asDynamic().resizeTo = element
    }

    val observer = ResizeObserver { _, _ ->
        // Gone with a destroyed app: the plugin clears it
        if (asDynamic().queueResize != null) queueResize()
    }

    observer.observe(element)

    return { observer.disconnect() }
}
