package io.peekandpoke.ultra.common

/**
 * Returns a new List with all occurrences of [old] replaced with [new].
 *
 * The comparison between the elements is done non strict '=='
 *
 * The receiver is not modified and the order of the remaining elements is kept.
 */
fun <E> List<E>.replace(old: E, new: E): List<E> = map { if (it == old) new else it }

/**
 * Returns a new List with all occurrences of [old] replaced with [new].
 *
 * The comparison between the elements is done strict '==='
 *
 * The receiver is not modified and the order of the remaining elements is kept.
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
 *
 * Named `without` rather than `remove` for two reasons: `remove` would be shadowed by the
 * `MutableList` member on a receiver typed as one, and that member drops only the FIRST occurrence
 * where this drops every one. `kotlin.collections.minus` is first-only as well.
 */
fun <E> List<E>.without(element: E): List<E> = filter { it != element }

/**
 * Returns a new List with all occurrences of [element] removed.
 *
 * The comparison between the elements is done strict '===' — see [without] for the naming.
 */
fun <E> List<E>.withoutStrict(element: E): List<E> = filter { it !== element }

/**
 * Returns a new List without the element at [idx].
 *
 * Throws [IndexOutOfBoundsException] for an [idx] outside `0 until size`.
 *
 * Named `withoutAt` rather than `removeAt`, which would be shadowed by the `MutableList` member on a
 * receiver typed as one — and that member mutates in place and returns the removed element instead.
 */
fun <E> List<E>.withoutAt(idx: Int) = toMutableList().apply { removeAt(idx) }.toList()

/**
 * Adds the [element] at [idx] by creating a new list with the element added.
 *
 * Throws [IndexOutOfBoundsException] for an [idx] outside `0..size`.
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
 *
 * The same holds for `idx1 == idx2`. In those no-op cases the receiver itself is returned,
 * not a copy - unlike every other branch of this function.
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

/**
 * Replaces the first item matched with [new] or adds it to the end of the list.
 *
 * All items are mapped by using [compareBy]. the comparison is done non-strict (==).
 *
 * Only the first match is replaced - further items with the same key stay untouched, so the
 * result can still hold duplicates by key. The receiver is not modified.
 */
fun <T, X> List<T>.replaceFirstByOrAdd(new: T, compareBy: (T) -> X): List<T> {

    val search = compareBy(new)

    return when (val idx = indexOfFirst { compareBy(it) == search }) {
        -1 -> plus(new)
        else -> replaceAt(idx, new)
    }
}
