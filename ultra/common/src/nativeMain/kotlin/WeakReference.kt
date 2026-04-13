package io.peekandpoke.ultra.common

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference as NativeWeakReference

@OptIn(ExperimentalNativeApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakReference<T> actual constructor(ref: T) {

    private var nativeRef: NativeWeakReference<Any>? =
        if (ref != null) NativeWeakReference(ref) else null

    actual val value: T? get() = get()

    @Suppress("UNCHECKED_CAST")
    actual fun get(): T? = nativeRef?.get() as? T

    actual fun clear() {
        nativeRef = null
    }
}
