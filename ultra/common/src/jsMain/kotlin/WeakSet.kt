package io.peekandpoke.ultra.common

/**
 * Object elements are matched by reference identity here, unlike the `equals`-based JVM and native
 * implementations. Primitive elements fall back to a strong Kotlin set and are matched structurally.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakSet<E> actual constructor() {
    /**
     * Weak storage for object-typed values (JS WeakSet only supports objects and symbols).
     *
     * A JS `WeakSet` is deliberately neither sized nor iterable, so entries stored here cannot be
     * counted or enumerated.
     */
    private var weakIds = createJsWeakSet()

    /**
     * Fallback storage for primitive-typed values that JS WeakSet rejects (strings, numbers, booleans).
     *
     * These are held strongly and are never released.
     */
    private val strongIds = mutableSetOf<E>()

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
