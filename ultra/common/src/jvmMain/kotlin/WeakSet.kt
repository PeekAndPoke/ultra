package io.peekandpoke.ultra.common

import java.util.Collections
import java.util.WeakHashMap

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakSet<E> actual constructor() {
    /**
     * Use a WeakHashMap-backed Set so keys can be garbage collected when no strong refs remain.
     *
     * `WeakHashMap` keys are matched with `equals`/`hashCode`, not by identity as on JS.
     */
    private val set: MutableSet<E> = Collections.newSetFromMap(WeakHashMap<E, Boolean>())

    actual fun contains(element: E): Boolean = set.contains(element)

    actual fun add(element: E) {
        set.add(element)
    }

    actual fun remove(element: E) {
        set.remove(element)
    }

    actual fun clear() = set.clear()
}
