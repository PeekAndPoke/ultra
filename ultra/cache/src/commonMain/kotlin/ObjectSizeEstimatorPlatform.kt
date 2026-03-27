package io.peekandpoke.ultra.cache

/**
 * Platform-specific helper for [ObjectSizeEstimatorImpl].
 *
 * Provides reflection-based field extraction so the estimator can
 * recursively walk arbitrary object graphs.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")
expect object ObjectSizeEstimatorPlatform {
    /**
     * Returns the field values of [obj] using platform reflection,
     * or `null` if reflection is unavailable.
     */
    fun getFieldsOf(obj: Any): List<Any?>?
}
