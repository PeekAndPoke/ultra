package de.peekandpoke.ultra.common

import java.util.*

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
// TODO: test me JVM
actual class WeakSet<E> actual constructor() {
    /**
     * Use a WeakHashMap-backed Set so keys can be garbage collected when no strong refs remain
     */
    private val set: MutableSet<E> = Collections.newSetFromMap(WeakHashMap<E, Boolean>())

    actual val size: Int get() = set.size

    actual fun toSet(): Set<E> = set.toSet()

    actual fun contains(element: E): Boolean = set.contains(element)

    actual fun add(element: E) {
        set.add(element)
    }

    actual fun remove(element: E) {
        set.remove(element)
    }

    actual fun clear() = set.clear()
}
