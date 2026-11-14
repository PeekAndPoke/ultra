package io.peekandpoke.ultra.cache

import java.util.IdentityHashMap

/** Backed by an [IdentityHashMap]: `System.identityHashCode` and `===`, constant time. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class IdentitySet actual constructor() {
    private val seen = IdentityHashMap<Any, Boolean>()

    actual fun add(obj: Any): Boolean = seen.put(obj, true) == null

    actual fun contains(obj: Any): Boolean = seen.containsKey(obj)
}
