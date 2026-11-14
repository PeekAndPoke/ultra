package io.peekandpoke.ultra.cache

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

/** A hash set keyed by `identityHashCode()` and `===`, constant time. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class IdentitySet actual constructor() {

    /** Wraps an object so the set compares by identity, never by the object's own equals. */
    private class ByIdentity(val obj: Any) {
        @OptIn(ExperimentalNativeApi::class)
        override fun hashCode(): Int = obj.identityHashCode()

        override fun equals(other: Any?): Boolean = other is ByIdentity && other.obj === obj
    }

    private val seen = HashSet<ByIdentity>()

    actual fun add(obj: Any): Boolean = seen.add(ByIdentity(obj))
}
