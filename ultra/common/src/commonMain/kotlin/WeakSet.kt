package io.peekandpoke.ultra.common

/**
 * A multiplatform set whose elements are held via weak references.
 *
 * Elements may be garbage collected when no strong references to them remain,
 * which will cause them to be silently removed from the set.
 *
 * The set is deliberately neither sizeable nor enumerable: a JS `WeakSet` supports neither, and an
 * answer that can go stale between asking and using it would be misleading anyway. Membership is
 * the only question it answers.
 *
 * Element matching is NOT uniform across platforms: JVM and native compare with `equals`, JS with
 * reference identity. Two equal-but-distinct instances are one element on JVM/native and two on JS.
 *
 * @param E The type of elements in the set.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")
expect class WeakSet<E>() {
    /** Returns `true` if the set contains the given [element]. */
    fun contains(element: E): Boolean

    /** Adds the given [element] to the set. */
    fun add(element: E)

    /** Removes the given [element] from the set. */
    fun remove(element: E)

    /** Removes all elements from the set. */
    fun clear()
}
