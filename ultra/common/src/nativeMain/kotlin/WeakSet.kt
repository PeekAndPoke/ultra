package io.peekandpoke.ultra.common

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference as NativeWeakReference

@OptIn(ExperimentalNativeApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakSet<E> actual constructor() {

    private val refs = mutableListOf<NativeWeakReference<Any>>()

    actual val size: Int
        get() {
            cleanup()
            return refs.size
        }

    @Suppress("UNCHECKED_CAST")
    actual fun toSet(): Set<E> {
        cleanup()
        return refs.mapNotNull { it.get() as? E }.toSet()
    }

    actual fun contains(element: E): Boolean {
        if (element == null) return false
        return refs.any { it.get() == element }
    }

    actual fun add(element: E) {
        if (element == null) return
        if (!contains(element)) {
            refs.add(NativeWeakReference(element))
        }
    }

    actual fun remove(element: E) {
        if (element == null) return
        refs.removeAll { it.get() == element }
    }

    actual fun clear() {
        refs.clear()
    }

    private fun cleanup() {
        refs.removeAll { it.get() == null }
    }
}
