package de.peekandpoke.ultra.common

/**
 * Replaces the [old] element with the [new] one.
 *
 * If [old] is found a new list instance is returned with the item replaced.
 * Otherwise the same list instance is returned.
 *
 * TODO: tests
 */
fun <E> List<E>.replace(old: E, new: E): List<E> = when (val idx = indexOf(old)) {
    -1 -> this
    else -> replaceAt(idx, new)
}

/**
 * Replaces the element at [idx] with [element] by creating a new list.
 *
 * TODO: tests
 */
fun <E> List<E>.replaceAt(idx: Int, element: E) = toMutableList().apply { set(idx, element) }.toList()

/**
 * Removes the [element] from the list.
 *
 * If the [element] is found a new list without the element is returned.
 * Otherwise the same list instance is returned.
 *
 * TODO: tests
 */
fun <E> List<E>.remove(element: E) = when (val idx = indexOf(element)) {
    -1 -> this
    else -> removeAt(idx) // TODO: remove all occurrences
}

/**
 * Removes the element at [idx] by creating a new list without the element.
 *
 * TODO: tests
 */
fun <E> List<E>.removeAt(idx: Int) = toMutableList().apply { removeAt(idx) }.toList()

/**
 * Adds the [element] at [idx] by creating a new list with the element added.
 *
 * TODO: tests
 */
fun <E> List<E>.addAt(idx: Int, element: E) = toMutableList().apply { add(idx, element) }.toList()

/**
 * Swaps the elements at [idx1] and [idx2] by creating a new list.
 *
 * TODO: tests
 */
fun <E> List<E>.swapAt(idx1: Int, idx2: Int): List<E> {

    if (idx1 < 0 || idx1 >= size || idx2 < 0 || idx2 >= size || idx1 == idx2) {
        return this
    }

    val mutable = toMutableList()

    val tmp = mutable[idx1]
    mutable[idx1] = mutable[idx2]
    mutable[idx2] = tmp

    return mutable.toList()
}
