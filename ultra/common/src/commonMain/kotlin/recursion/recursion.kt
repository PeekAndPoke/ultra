package io.peekandpoke.ultra.common.recursion

/**
 * Walks up a parent chain starting from this element, collecting all ancestors into a list.
 *
 * The traversal stops when [getParent] returns null or when a cycle is detected.
 * The returned list starts with this element and ends with the root.
 *
 * Cycle detection compares already-collected elements with `equals`, not by identity, so a chain
 * that revisits an equal-but-distinct element stops there as well.
 */
fun <T> T.recurse(getParent: T.() -> T?): List<T> {

    val result = mutableListOf<T>()
    // a set alongside the list, so the cycle check is a hash lookup rather than a scan of everything
    // collected so far - the walk stays linear instead of quadratic
    val seen = mutableSetOf<T>()

    var current: T? = this

    while (current != null && seen.add(current)) {
        result.add(current)

        current = current.getParent()
    }

    return result
}

/**
 * Flattens a tree rooted at this element into a set by recursively visiting all [children].
 *
 * Each node is visited at most once; cycles are detected and skipped. Nodes are compared with
 * `equals`, so equal-but-distinct nodes collapse into a single entry.
 *
 * The result is a fresh set in depth-first pre-order, starting with this element. Traversal uses
 * the call stack, so its depth grows with the depth of the tree.
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
