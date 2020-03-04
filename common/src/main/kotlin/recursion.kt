package de.peekandpoke.ultra.common

/**
 *
 */
fun <T> T.recurse(getParent: T.() -> T?): List<T> {

    val result = mutableListOf<T>()

    var current: T? = this

    while (current != null && current !in result) {
        result.add(current)

        current = current.getParent()
    }

    return result
}
