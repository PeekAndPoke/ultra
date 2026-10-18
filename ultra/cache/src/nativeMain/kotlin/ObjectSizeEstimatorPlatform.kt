package io.peekandpoke.ultra.cache

/** Native implementation — returns `null` because Kotlin/Native lacks runtime reflection. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ObjectSizeEstimatorPlatform {
    /** Always returns `null` — the caller then falls back to a flat per-object charge. */
    // TODO(scan): every custom object therefore estimates to the same constant on native,
    //  regardless of what it holds, which makes a byte-budgeted cache unusable there.
    actual fun getFieldsOf(obj: Any): List<Any?>? {
        // Native does not provide runtime reflection for walking object fields.
        return null
    }
}
