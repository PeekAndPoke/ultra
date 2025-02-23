package de.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class WeakReference<T>(ref: T) {
    val value: T?

    fun get(): T?

    fun clear()
}
