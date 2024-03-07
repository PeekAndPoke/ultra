package de.peekandpoke.ultra.common

import java.lang.ref.WeakReference as JavaWeakReference

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WeakReference<T> actual constructor(ref: T) {

    private var internal: JavaWeakReference<T>? = JavaWeakReference(ref)

    actual val value: T? get() = get()

    actual fun get(): T? = internal?.get()

    actual fun clear() {
        internal?.clear()
        internal = null
    }
}
