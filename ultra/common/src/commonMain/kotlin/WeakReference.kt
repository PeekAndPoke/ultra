package io.peekandpoke.ultra.common

/**
 * Multiplatform weak reference wrapper.
 *
 * Holds a reference to [ref] that does not prevent garbage collection of the referent.
 *
 * @param T The type of the referenced object.
 * @param ref The object to reference weakly.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class WeakReference<T>(ref: T) {
    /** Returns the referenced object, or null if it has been garbage collected. */
    val value: T?

    /** Returns the referenced object, or null if it has been garbage collected. */
    fun get(): T?

    /** Clears this weak reference so that [get] and [value] will return null. */
    fun clear()
}
