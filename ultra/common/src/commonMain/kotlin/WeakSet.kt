package io.peekandpoke.ultra.common

/**
 * A multiplatform set whose elements are held via weak references.
 *
 * Elements may be garbage collected when no strong references to them remain,
 * which will cause them to be silently removed from the set.
 *
 * @param E The type of elements in the set.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")
expect class WeakSet<E>() {
    /** The current number of elements in the set. */
    val size: Int

    /** Returns a snapshot of the elements as an immutable [Set]. */
    fun toSet(): Set<E>

    /** Returns `true` if the set contains the given [element]. */
    fun contains(element: E): Boolean

    /** Adds the given [element] to the set. */
    fun add(element: E)

    /** Removes the given [element] from the set. */
    fun remove(element: E)

    /** Removes all elements from the set. */
    fun clear()
}

/** Returns `true` if this [WeakSet] contains no elements. */
fun <E> WeakSet<E>.isEmpty() = size == 0

/** Returns `true` if this [WeakSet] contains at least one element. */
fun <E> WeakSet<E>.isNotEmpty() = !isEmpty()
