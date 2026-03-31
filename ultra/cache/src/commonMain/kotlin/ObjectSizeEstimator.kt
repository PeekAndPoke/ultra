package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.cache.ObjectSizeEstimatorImpl.EstimatorConfig
import io.peekandpoke.ultra.common.WeakSet

/**
 * Estimates the in-memory size of an object graph in bytes.
 *
 * Used by [FastCache.MaxMemoryUsageBehaviour] to track cumulative memory
 * consumption and trigger eviction when a threshold is exceeded.
 */
interface ObjectSizeEstimator {
    companion object {
        /** Creates a default [ObjectSizeEstimatorImpl] with the given [cfg]. */
        operator fun invoke(cfg: EstimatorConfig = EstimatorConfig()): ObjectSizeEstimator {
            return ObjectSizeEstimatorImpl(cfg)
        }
    }

    /** Returns the estimated size of [obj] in bytes, traversing the object graph recursively. */
    fun estimate(obj: Any?): Long
}

/**
 * Default [ObjectSizeEstimator] implementation.
 *
 * Walks the object graph using platform-specific reflection
 * (see [ObjectSizeEstimatorPlatform]) and sums up estimated sizes for
 * primitives, strings, arrays, collections, maps, and arbitrary objects.
 *
 * A [WeakSet] guards against infinite loops caused by circular references.
 *
 * @param cfg tuning knobs for header and pointer sizes
 */
class ObjectSizeEstimatorImpl(
    val cfg: EstimatorConfig = EstimatorConfig(),
) : ObjectSizeEstimator {
    companion object {
        /** Estimated size of a null reference. */
        const val NULL_SIZE = 4L

        /** Estimated size of a [Boolean] value. */
        const val BOOL_SIZE = 1L

        /** Estimated size of a [Byte] value. */
        const val BYTE_SIZE = 1L

        /** Estimated size of a [Char] value. */
        const val CHAR_SIZE = 2L

        /** Estimated size of a [Short] value. */
        const val SHORT_SIZE = 2L

        /** Estimated size of an [Int] value. */
        const val INT_SIZE = 4L

        /** Estimated size of a [Long] value. */
        const val LONG_SIZE = 8L

        /** Estimated size of a [Float] value. */
        const val FLOAT_SIZE = 4L

        /** Estimated size of a [Double] value. */
        const val DOUBLE_SIZE = 8L
    }

    private val seen = WeakSet<Any?>()

    /**
     * Configuration for the size-estimation heuristics.
     *
     * @property objectHeader overhead per heap object (e.g. 16 bytes on HotSpot 64-bit)
     * @property arrayHeader  overhead per array object (includes length field)
     * @property pointerSize  size of a single reference / pointer
     */
    data class EstimatorConfig(
        val objectHeader: Long = 16L,
        val arrayHeader: Long = 24L,
        val pointerSize: Long = 8L,
    )

    override fun estimate(obj: Any?): Long {
        // Primitives: no cycle detection needed (they can't form reference cycles,
        // and JS WeakSet does not support primitive values)
        when (obj) {
            null -> return NULL_SIZE
            is Boolean -> return BOOL_SIZE
            is Byte -> return BYTE_SIZE
            is Char -> return CHAR_SIZE
            is Short -> return SHORT_SIZE
            is Int -> return INT_SIZE
            is Long -> return LONG_SIZE
            is Float -> return FLOAT_SIZE
            is Double -> return DOUBLE_SIZE
        }

        // For reference types: avoid cycles
        if (seen.contains(obj)) return 0L
        seen.add(obj)

        return when (obj) {
            is String -> {
                val chars = obj.length.toLong() * CHAR_SIZE
                // return
                cfg.objectHeader + cfg.pointerSize + cfg.arrayHeader + chars
            }

            is BooleanArray -> cfg.arrayHeader + BOOL_SIZE * obj.size
            is ByteArray -> cfg.arrayHeader + BYTE_SIZE * obj.size
            is ShortArray -> cfg.arrayHeader + SHORT_SIZE * obj.size
            is CharArray -> cfg.arrayHeader + CHAR_SIZE * obj.size
            is IntArray -> cfg.arrayHeader + INT_SIZE * obj.size
            is LongArray -> cfg.arrayHeader + LONG_SIZE * obj.size
            is FloatArray -> cfg.arrayHeader + FLOAT_SIZE * obj.size
            is DoubleArray -> cfg.arrayHeader + DOUBLE_SIZE * obj.size

            is Array<*> -> {
                var sum = cfg.arrayHeader + obj.size.toLong() * cfg.pointerSize
                for (e in obj) sum += estimate(e)
                // return
                sum
            }

            is Collection<*> -> {
                var sum = cfg.objectHeader + obj.size.toLong() * cfg.pointerSize
                for (e in obj) sum += estimate(e)
                // return
                sum
            }

            is Map<*, *> -> {
                var sum = cfg.objectHeader + obj.size.toLong() * 2 * cfg.pointerSize
                for ((k, v) in obj) {
                    sum += estimate(k)
                    sum += estimate(v)
                }
                // return
                sum
            }

            else -> estimateObject(obj)
        }
    }

    private fun estimateObject(obj: Any): Long {

        val fields = ObjectSizeEstimatorPlatform.getFieldsOf(obj)
            ?: return cfg.objectHeader + 2L * cfg.pointerSize // No reflection (e.g., Native): charge a plain object.

        var sum = cfg.objectHeader + fields.size.toLong() * cfg.pointerSize
        for (f in fields) sum += estimate(f)

        return sum
    }
}
