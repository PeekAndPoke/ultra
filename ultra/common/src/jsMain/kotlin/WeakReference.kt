package io.peekandpoke.ultra.common

// Original JS reference
private external class WeakRef<T>(@Suppress("unused") target: T) {
    fun deref(): T
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakReference<T> actual constructor(ref: T) {

    /** Weak reference for object-typed values. */
    private var weakRef: WeakRef<T>? = if (isWeakRefCompatible(ref)) WeakRef(ref) else null

    /** Strong fallback for primitive-typed values that JS WeakRef rejects. */
    private var strongRef: T? = if (isWeakRefCompatible(ref)) null else ref

    actual val value: T? get() = get()

    actual fun get(): T? = weakRef?.deref() ?: strongRef

    actual fun clear() {
        weakRef = null
        strongRef = null
    }
}
