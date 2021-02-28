package de.peekandpoke.ultra.common

/**
 * Returns a new List with all occurrences of [old] replaced with [new].
 *
 * The comparison between the elements is done non strict '=='
 */
fun <E> List<E>.replace(old: E, new: E): List<E> = map { if (it == old) new else it }

/**
 * Returns a new List with all occurrences of [old] replaced with [new].
 *
 * The comparison between the elements is done strict '==='
 */
fun <E> List<E>.replaceStrict(old: E, new: E): List<E> = map { if (it === old) new else it }

/**
 * Returns a new List with the element at [idx] replaced by [element].
 *
 * If the index is out of bounds an [IndexOutOfBoundsException] is thrown.
 */
fun <E> List<E>.replaceAt(idx: Int, element: E): List<E> = toMutableList().apply { set(idx, element) }.toList()

/**
 * Returns a new List with all occurrences of [element] removed.
 *
 * The comparison between the elements is done non strict '=='
 */
fun <E> List<E>.remove(element: E): List<E> = filter { it != element }

/**
 * Returns a new List with all occurrences of [element] removed.
 *
 * The comparison between the elements is done strict '==='
 */
fun <E> List<E>.removeStrict(element: E): List<E> = filter { it !== element }

/**
 * Removes the element at [idx] by creating a new list without the element.
 */
fun <E> List<E>.removeAt(idx: Int) = toMutableList().apply { removeAt(idx) }.toList()

/**
 * Adds the [element] at [idx] by creating a new list with the element added.
 */
fun <E> List<E>.addAt(idx: Int, element: E) = toMutableList().apply { add(idx, element) }.toList()

/**
 * Adds the [element] at the beginning by creating a new list with the element added.
 */
fun <E> List<E>.prepend(element: E) = addAt(0, element)

/**
 * Swaps the elements at [idx1] and [idx2] by creating a new list.
 *
 * If idx1 or idx2 is out of bounds, the list is returned as is.
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
