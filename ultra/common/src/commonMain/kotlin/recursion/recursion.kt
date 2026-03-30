package io.peekandpoke.ultra.common.recursion

/**
 * Walks up a parent chain starting from this element, collecting all ancestors into a list.
 *
 * The traversal stops when [getParent] returns null or when a cycle is detected.
 * The returned list starts with this element and ends with the root.
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
 * Flattens a tree rooted at this element into a set by recursively visiting all [children].
 *
 * Each node is visited at most once; cycles are detected and skipped.
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
