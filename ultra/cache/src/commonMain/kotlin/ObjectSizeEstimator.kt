package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.cache.ObjectSizeEstimatorImpl.EstimatorConfig

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
 * An identity set of already-visited objects guards against infinite loops caused by circular
 * references. It is built per call, so an estimate never depends on what was measured before.
 *
 * @param cfg tuning knobs for header and pointer sizes
 */
class ObjectSizeEstimatorImpl(
    /** Configuration for header, array, and pointer size heuristics. */
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

    /**
     * Tracks the objects visited during one walk, by identity.
     *
     * Buckets by `hashCode` but compares with `===`, so two equal-but-distinct objects are counted
     * separately — each really occupies memory. Structural matching would report the second as
     * already-seen and charge it zero bytes.
     */
    private class VisitedSet {
        private val buckets = mutableMapOf<Int, MutableList<Any>>()

        /** Records [obj] and returns true when it had not been visited before. */
        fun add(obj: Any): Boolean {
            val bucket = buckets.getOrPut(obj.hashCode()) { mutableListOf() }

            if (bucket.any { it === obj }) {
                return false
            }

            bucket.add(obj)

            return true
        }
    }

    /**
     * Estimates the in-memory size of [obj] in bytes, recursively traversing the object graph.
     *
     * Each call starts with an empty visited set, so repeated calls on the same object return the
     * same answer and one estimate cannot shrink another.
     */
    override fun estimate(obj: Any?): Long = estimate(obj, VisitedSet())

    private fun estimate(obj: Any?, seen: VisitedSet): Long {
        // Primitives: no cycle detection needed, they cannot form reference cycles
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

        // For reference types: avoid cycles. Already visited means already counted.
        if (!seen.add(obj)) return 0L

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
                for (e in obj) sum += estimate(e, seen)
                // return
                sum
            }

            is Collection<*> -> {
                var sum = cfg.objectHeader + obj.size.toLong() * cfg.pointerSize
                for (e in obj) sum += estimate(e, seen)
                // return
                sum
            }

            is Map<*, *> -> {
                var sum = cfg.objectHeader + obj.size.toLong() * 2 * cfg.pointerSize
                for ((k, v) in obj) {
                    sum += estimate(k, seen)
                    sum += estimate(v, seen)
                }
                // return
                sum
            }

            else -> estimateObject(obj, seen)
        }
    }

    private fun estimateObject(obj: Any, seen: VisitedSet): Long {

        val fields = ObjectSizeEstimatorPlatform.getFieldsOf(obj)
            ?: return cfg.objectHeader + 2L * cfg.pointerSize // No reflection (e.g., Native): charge a plain object.

        var sum = cfg.objectHeader + fields.size.toLong() * cfg.pointerSize
        for (f in fields) sum += estimate(f, seen)

        return sum
    }
}
