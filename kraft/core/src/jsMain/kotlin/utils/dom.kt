package io.peekandpoke.kraft.utils

import kotlinx.browser.document
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement

fun HTMLElement?.absolutePosition(): Vector2D = this?.let { absolutePosition() } ?: Vector2D.zero

fun HTMLElement.absolutePosition(): Vector2D {

    val bounds = getBoundingClientRect()

    return Vector2D(bounds.left, bounds.top)
}

fun HTMLElement.getPageCoords(): Rectangle {
    val body = document.body?.getBoundingClientRect() ?: DOMRect()
    val rect = getBoundingClientRect()

    return Rectangle(
        x1 = (rect.left - body.left),
        y1 = (rect.top - body.top),
        width = rect.width,
        height = rect.height,
    )
}
