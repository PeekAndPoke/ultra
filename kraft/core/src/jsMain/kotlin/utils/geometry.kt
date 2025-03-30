package de.peekandpoke.kraft.utils

data class Vector2D(val x: Double, val y: Double) {
    companion object {
        val zero = Vector2D(0.0, 0.0)
    }

    operator fun plus(other: Vector2D) = Vector2D(x = x + other.x, y = y + other.y)

    operator fun minus(other: Vector2D) = Vector2D(x = x - other.x, y = y - other.y)

    operator fun div(value: Double) = Vector2D(x = x / value, y = y / value)

    operator fun times(value: Double) = Vector2D(x = x * value, y = y * value)
}

data class Rectangle(val x1: Double, val y1: Double, val width: Double, val height: Double) {
    val topLeft by lazy { Vector2D(x1, y1) }
    val bottomLeft by lazy { Vector2D(x1, y1 + height) }
    val topRight by lazy { Vector2D(x1 + width, y1) }
    val bottomRight by lazy { Vector2D(x1 + width, y1 + height) }
}
