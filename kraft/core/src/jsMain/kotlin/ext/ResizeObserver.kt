@file:Suppress("unused")

package de.peekandpoke.kraft.ext

import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element

// --- Resize Observer ---

external class ResizeObserver(callback: (entries: Array<ResizeObserverEntry>, observer: ResizeObserver) -> Unit) {
    fun observe(target: Element, options: dynamic = definedExternally)
    fun unobserve(target: Element)
    fun disconnect()
}

external interface ResizeObserverEntry {
    val target: Element
    val contentRect: DOMRectReadOnly
    val borderBoxSize: Array<ResizeObserverSize>
    val contentBoxSize: Array<ResizeObserverSize>
    val devicePixelContentBoxSize: Array<ResizeObserverSize>
}

external interface ResizeObserverSize {
    val inlineSize: Double
    val blockSize: Double
}
