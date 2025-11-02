package de.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
// // TODO: test me JVM
expect class WeakSet<T>() {
    fun has(value: T): Boolean

    fun put(value: T)

    fun remove(value: T)

    fun clear()
}
