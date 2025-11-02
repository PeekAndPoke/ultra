package de.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
// TODO: test me JS
actual class WeakSet<T> actual constructor() {
    private var ids = createJsWeakSet()

    actual fun has(value: T): Boolean = ids.has(value)

    actual fun put(value: T) {
        ids.add(value)
    }

    actual fun remove(value: T) {
        ids.delete(value)
    }

    actual fun clear() {
        ids = createJsWeakSet()
    }

    private fun createJsWeakSet(): dynamic = js("new WeakSet()")
}
