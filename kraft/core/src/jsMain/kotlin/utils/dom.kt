package de.peekandpoke.kraft.utils

import org.w3c.dom.HTMLElement

fun HTMLElement?.absolutePosition(): Vector2D = this?.let { absolutePosition() } ?: Vector2D.zero

fun HTMLElement.absolutePosition(): Vector2D {

    val bounds = getBoundingClientRect()

    return Vector2D(bounds.left, bounds.top)
}
