package io.peekandpoke.ultra.common

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference as NativeWeakReference

@OptIn(ExperimentalNativeApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakSet<E> actual constructor() {

    /**
     * Backing list of weak references, scanned linearly and matched with `equals`.
     *
     * All operations are therefore O(n), and a null element is silently ignored.
     */
    private val refs = mutableListOf<NativeWeakReference<Any>>()

    actual fun contains(element: E): Boolean {
        if (element == null) return false
        return refs.any { it.get() == element }
    }

    actual fun add(element: E) {
        if (element == null) return
        cleanup()
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

    /** Drops references whose referent has been collected. */
    private fun cleanup() {
        refs.removeAll { it.get() == null }
    }
}
