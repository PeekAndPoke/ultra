package io.peekandpoke.kraft.utils

/** A 2D vector with [x] and [y] components, supporting basic arithmetic. */
data class Vector2D(val x: Double, val y: Double) {
    companion object {
        /** The zero vector (0, 0). */
        val zero = Vector2D(0.0, 0.0)
    }

    operator fun plus(other: Vector2D) = Vector2D(x = x + other.x, y = y + other.y)

    operator fun minus(other: Vector2D) = Vector2D(x = x - other.x, y = y - other.y)

    operator fun div(value: Double) = Vector2D(x = x / value, y = y / value)

    operator fun times(value: Double) = Vector2D(x = x * value, y = y * value)
}

/** An axis-aligned rectangle defined by its top-left corner ([x1], [y1]) and dimensions ([width], [height]). */
data class Rectangle(val x1: Double, val y1: Double, val width: Double, val height: Double) {
    val topLeft by lazy { Vector2D(x1, y1) }
    val bottomLeft by lazy { Vector2D(x1, y1 + height) }
    val topRight by lazy { Vector2D(x1 + width, y1) }
    val bottomRight by lazy { Vector2D(x1 + width, y1 + height) }

    /** The center point of the rectangle. */
    val center by lazy { topLeft.plus(Vector2D(width / 2, height / 2)) }

    /** The width and height as a [Vector2D]. */
    val dimension by lazy { Vector2D(width, height) }
}
