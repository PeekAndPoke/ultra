package de.peekandpoke.ultra.common.recursion

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

/**
 *
 */
fun <T> T.flattenTreeToSet(children: (T) -> List<T>): Set<T> {

    val result = mutableSetOf<T>()

    fun visit(element: T) {
        if (result.contains(element)) {
            return
        }

        result.add(element)

        children(element).forEach {
            visit(it)
        }
    }

    visit(this)

    return result.toSet()
}
