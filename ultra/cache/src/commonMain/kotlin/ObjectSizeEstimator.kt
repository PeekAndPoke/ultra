package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.cache.ObjectSizeEstimatorImpl.EstimatorConfig

/**
 * Estimates the in-memory size of an object graph in bytes.
 *
 * Used by [FastCache.MaxMemoryUsageBehaviour] to track cumulative memory
 * consumption and trigger eviction when a threshold is exceeded.
 *
 * ## Accuracy — read this before relying on a byte budget
 *
 * The result is a rough heuristic, and its error is not uniform across platforms. Measured
 * 2026-07-30; every figure below is reproducible, and the known-wrong cases are recorded in
 * `.claude/tasks/20260730-cache-scan-findings.md` rather than fixed. **Treat
 * [FastCache.Builder.maxMemoryUsage] as a relative pressure signal, not a guarantee.**
 *
 * - **Native: no per-object detail at all.** `ObjectSizeEstimatorPlatform.getFieldsOf` returns
 *   `null`, so *every* custom object is charged a flat `objectHeader + 2 * pointerSize` = 32 bytes.
 *   A data class holding a 10 MB `ByteArray` is charged 32 bytes, so a memory bound never fires.
 * - **JS: every number costs 1 byte.** `Int`, `Short`, `Float` and `Double` are all a JS `number`,
 *   which the `is Byte` branch matches first — an 8x under-estimate for numeric payloads.
 * - **JVM: collections and maps are under-counted several-fold.** A map is charged
 *   `objectHeader + 2n * pointerSize`, i.e. 16 bytes per entry, against roughly 48 real once the
 *   backing table, its growth slack and each `LinkedHashMap.Entry` are counted. Elements are also
 *   charged their raw value size rather than the box.
 * - **JVM: types whose fields are inaccessible are charged 16 bytes.** Under JPMS `setAccessible`
 *   throws and the field is dropped, so `Instant`, `LocalDate`, `UUID`, `BigDecimal`,
 *   `StringBuilder` and `ByteBuffer` all estimate as an empty object regardless of what they hold.
 *
 * Errors do not cancel: `String` is over-counted under compact strings, and a key shared with a
 * value is charged twice — both in the safe direction, the ones above are not.
 */
interface ObjectSizeEstimator {
    /** Factory for the default implementation. */
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
 * An identity set of already-visited objects keeps a shared sub-object from being charged twice.
 * It is built per call, so an estimate never depends on what was measured before.
 *
 * The result is a heuristic, not a measurement: the default [EstimatorConfig] describes a
 * 64-bit HotSpot heap and is used unchanged on JS and native.
 *
 * @param cfg tuning knobs for header and pointer sizes
 */
class ObjectSizeEstimatorImpl(
    /** Configuration for header, array, and pointer size heuristics. */
    val cfg: EstimatorConfig = EstimatorConfig(),
) : ObjectSizeEstimator {
    /** Per-value size constants used by [ObjectSizeEstimatorImpl.estimate]. */
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
     * Compares with `===`, so two equal-but-distinct objects are counted separately — each really
     * occupies memory. Structural matching would report the second as already-seen and charge it
     * zero bytes.
     *
     * **Never calls `hashCode()` or `equals()` on the tracked objects.** Doing so defeats the whole
     * purpose: a cyclic graph of data classes, `List`s or `Map`s hashes recursively, so the guard
     * would overflow the stack on exactly the input it exists to detect. That also keeps a
     * user-supplied `hashCode()` off the cache's hot path.
     */
    private class VisitedSet {
        // Linear scan by identity: O(n^2) in graph size, traded for correctness. A keyed structure
        // would have to hash its keys, which is the defect above; an identity-keyed set needs an
        // expect/actual (IdentityHashMap / JS Set / native fallback) and is tracked as a follow-up.
        // The practical bound is the recursion depth the stack allows, since estimate() has none.
        private val seen = mutableListOf<Any>()

        /** Records [obj] and returns true when it had not been visited before. */
        fun add(obj: Any): Boolean {
            if (seen.any { it === obj }) {
                return false
            }

            seen.add(obj)

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
        // TODO(scan): on JS every number is a JS `number`, so the `is Byte` branch swallows Int,
        //  Short, Float and Double and charges all of them 1 byte.
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

        // TODO(scan): the recursion below has no depth bound, so a deep graph (long linked list,
        //  deeply nested collections) overflows the stack out of a cache put/read.
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

            // TODO(scan): a collection/map is charged one (or two) pointers per entry, which omits
            //  the backing array and the per-entry node objects, and an element that is a boxed
            //  primitive is charged its raw value size. Both under-count on the JVM.
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

        // TODO(scan): the `2L` is unexplained, and on native this branch is taken for EVERY custom
        //  object, so a data class holding megabytes is charged a flat 32 bytes.
        val fields = ObjectSizeEstimatorPlatform.getFieldsOf(obj)
            ?: return cfg.objectHeader + 2L * cfg.pointerSize // No reflection (e.g., Native): charge a plain object.

        var sum = cfg.objectHeader + fields.size.toLong() * cfg.pointerSize
        for (f in fields) sum += estimate(f, seen)

        return sum
    }
}
