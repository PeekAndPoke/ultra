package io.peekandpoke.ultra.common

/**
 * The ES2021 global `WeakRef`.
 *
 * `deref()` yields `undefined` once the target is collected, which Kotlin reads as `null`.
 */
private external class WeakRef<T>(@Suppress("unused") target: T) {
    fun deref(): T
}

/**
 * Requires the ES2021 `WeakRef` global; constructing one on a runtime without it fails.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakReference<T> actual constructor(ref: T) {

    /** Weak reference for object-typed values. */
    private var weakRef: WeakRef<T>? = if (isWeakRefCompatible(ref)) WeakRef(ref) else null

    /**
     * Strong fallback for primitive-typed values that JS WeakRef rejects.
     *
     * Such a value is pinned for the lifetime of this instance — [get] never starts returning null.
     */
    private var strongRef: T? = if (isWeakRefCompatible(ref)) null else ref

    actual val value: T? get() = get()

    actual fun get(): T? = weakRef?.deref() ?: strongRef

    actual fun clear() {
        weakRef = null
        strongRef = null
    }
}
