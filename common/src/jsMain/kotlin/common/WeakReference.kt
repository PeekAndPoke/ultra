package de.peekandpoke.ultra.common

// Original JS reference
private external class WeakRef<T>(target: T) {
    fun deref(): T
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakReference<T> actual constructor(ref: T) {

    private var internal: WeakRef<T>? = WeakRef(ref)

    actual val value: T? get() = get()

    actual fun get(): T? = internal?.deref()

    actual fun clear() {
        internal = null
    }
}
