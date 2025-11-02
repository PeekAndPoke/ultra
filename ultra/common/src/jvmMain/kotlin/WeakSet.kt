package de.peekandpoke.ultra.common

import java.util.*

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
// TODO: test me JVM
actual class WeakSet<T> actual constructor() {
    private val ids = IdentityHashMap<T, Boolean>()

    actual fun has(value: T): Boolean = ids.containsKey(value)

    actual fun put(value: T) {
        ids[value] = true
    }

    actual fun remove(value: T) {
        ids.remove(value)
    }

    actual fun clear() {
        ids.clear()
    }
}
