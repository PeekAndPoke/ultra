package io.peekandpoke.kraft.addons.pixijs

import io.peekandpoke.kraft.addons.pixijs.js.Application
import io.peekandpoke.kraft.ext.ResizeObserver
import org.w3c.dom.HTMLElement

/**
 * Keeps the app the size of [element], also when layout alone resizes it (a panel docked under it, a splitter drag),
 * which `resizeTo` does not notice: it follows the window's `resize` event only (ULTRA-00262, from Klang). A
 * `ResizeObserver` asks for [Application.queueResize], so several changes in one frame resize once. Sets `resizeTo` to
 * [element]. Call it after `init` (before, pixi's ResizePlugin is not there yet). Returns the stop; a destroyed app
 * stops it too.
 */
fun Application.followElementSize(element: HTMLElement): () -> Unit {
    require(asDynamic().queueResize != null) { "followElementSize needs an initialized Application: call it after init()" }

    if (asDynamic().resizeTo !== element) {
        asDynamic().resizeTo = element
    }

    lateinit var observer: ResizeObserver

    observer = ResizeObserver { _, _ ->
        // A destroyed app: the plugin cleared queueResize
        if (asDynamic().queueResize == null) observer.disconnect() else queueResize()
    }

    observer.observe(element)

    return { observer.disconnect() }
}
