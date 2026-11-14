package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.cache.ObjectSizeEstimatorImpl.EstimatorConfig

/** Native implementation — returns `null` because Kotlin/Native lacks runtime reflection. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ObjectSizeEstimatorPlatform {
    /** Always returns `null` — the caller then falls back to a flat per-object charge. */
    actual fun getFieldsOf(obj: Any): List<Any?>? {
        // Native does not provide runtime reflection for walking object fields.
        return null
    }

    /** Always `null`: no reflection. */
    internal actual fun fieldsForWalk(obj: Any): WalkFields? = null

    /** Nothing beyond the common strings and primitive arrays. */
    internal actual fun leafSizeOf(obj: Any, cfg: EstimatorConfig): Long? = null
}
