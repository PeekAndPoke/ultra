@file:Suppress("unused")

package de.peekandpoke.kraft.ext

import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element

// --- Intersection Observer ---

external class IntersectionObserver(
    callback: (entries: Array<IntersectionObserverEntry>, observer: IntersectionObserver) -> Unit,
    options: dynamic = definedExternally,
) {
    fun observe(target: Element)
    fun unobserve(target: Element)
    fun disconnect()
}

external interface IntersectionObserverEntry {
    val isIntersecting: Boolean
    val intersectionRatio: Double
    val target: Element
    val boundingClientRect: DOMRectReadOnly
    val intersectionRect: DOMRectReadOnly
    val rootBounds: DOMRectReadOnly?
    val time: Double
}
