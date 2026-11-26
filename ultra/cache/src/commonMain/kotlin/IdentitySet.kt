package io.peekandpoke.ultra.cache

/**
 * A set of objects by identity, for the size estimator's walk.
 *
 * Never calls `hashCode()` or `equals()` of an object it holds: on a cyclic graph of data classes or
 * collections those recurse into the very cycle the set exists to detect, and they are user code.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect class IdentitySet() {
    /** Records [obj]; returns true when it was not in the set yet. */
    fun add(obj: Any): Boolean

    /** Whether [obj] is in the set. */
    fun contains(obj: Any): Boolean
}
