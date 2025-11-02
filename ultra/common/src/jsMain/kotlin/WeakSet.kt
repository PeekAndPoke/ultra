package de.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
// TODO: test me JS
actual class WeakSet<E> actual constructor() {
    private var ids = createJsWeakSet()

    actual val size: Int get() = ids.size

    actual fun toSet(): Set<E> = (ids.values().toArray() as Array<E>).toSet()

    actual fun contains(element: E): Boolean = ids.has(element)

    actual fun add(element: E) {
        ids.add(element)
    }

    actual fun remove(element: E) {
        ids.delete(element)
    }

    actual fun clear() {
        ids = createJsWeakSet()
    }

    private fun createJsWeakSet(): dynamic = js("new WeakSet()")
}
