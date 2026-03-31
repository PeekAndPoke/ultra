package io.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakSet<E> actual constructor() {
    /** Weak storage for object-typed values (JS WeakSet only supports objects and symbols). */
    private var weakIds = createJsWeakSet()

    /** Fallback storage for primitive-typed values that JS WeakSet rejects (strings, numbers, booleans). */
    private val strongIds = mutableSetOf<E>()

    actual val size: Int get() = weakIds.size + strongIds.size

    actual fun toSet(): Set<E> {
        @Suppress("UNCHECKED_CAST")
        val weak = (weakIds.values().toArray() as Array<E>).toSet()
        return weak + strongIds
    }

    actual fun contains(element: E): Boolean {
        return if (isWeakRefCompatible(element)) weakIds.has(element) else strongIds.contains(element)
    }

    actual fun add(element: E) {
        if (isWeakRefCompatible(element)) weakIds.add(element) else strongIds.add(element)
    }

    actual fun remove(element: E) {
        if (isWeakRefCompatible(element)) weakIds.delete(element) else strongIds.remove(element)
    }

    actual fun clear() {
        weakIds = createJsWeakSet()
        strongIds.clear()
    }

    private fun createJsWeakSet(): dynamic = js("new WeakSet()")
}
