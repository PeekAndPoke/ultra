package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.cache.ObjectSizeEstimatorImpl.EstimatorConfig

/**
 * Platform-specific helper for [ObjectSizeEstimatorImpl]: the fields of an object, by reflection where
 * the platform has it.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")
expect object ObjectSizeEstimatorPlatform {
    /**
     * Returns the field values of [obj] using platform reflection, or `null` if reflection is
     * unavailable.
     *
     * The order is unspecified, and what counts as a field differs per platform - see the `actual`
     * declarations. A value that could not be read is an internal marker, not the field's value.
     */
    fun getFieldsOf(obj: Any): List<Any?>?

    /** The fields of [obj] for the walk, read only at the positions it samples; `null` without reflection. */
    internal fun fieldsForWalk(obj: Any): WalkFields?

    /** The size of a platform value that is sized from its length without descent, or `null`. */
    internal fun leafSizeOf(obj: Any, cfg: EstimatorConfig): Long?
}
