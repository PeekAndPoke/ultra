package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.cache.ObjectSizeEstimatorImpl.EstimatorConfig
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Estimates the in-memory size of an object graph in bytes.
 *
 * Used by [FastCache.MaxMemoryUsageBehaviour] to track the memory a cache holds and evict when a budget
 * is exceeded. **Treat [FastCache.Builder.maxMemoryUsage] as a pressure signal, not a guarantee**: the
 * default estimator trades accuracy for a bounded cost, because it runs on the cache's maintenance loop.
 *
 * ## What the default estimator counts
 *
 * Primitives by size; a string as header, reference, array header and two bytes per char; arrays as
 * array header plus elements; a collection as header plus one reference per element, a map two, plus
 * what the elements hold; any other object as header plus one reference per field, plus what the fields
 * hold (JVM: reflection, JS: own properties, native: nothing - see below). An object reached twice is
 * counted once.
 *
 * ## Bounded cost, and what it costs in accuracy
 *
 * - **Sampling.** A collection, map or array with more than 64 elements (`sampleSize`) is not walked
 *   whole: 64 elements are, and the rest is charged by their average. The positions are fixed, never
 *   random, so the same value always gets the same estimate; by default they follow the golden ratio
 *   (`SamplePick.GoldenRatio`), spread evenly without a fixed period. `SamplePick.EvenlySpaced`, a
 *   fixed stride, is the alternative - switch with `EstimatorConfig(samplePick = ...)`. A collection
 *   without index access (a set, a linked list, a map) is sampled over its first 1 024 elements only
 *   (`sampleWindow`), to keep the stepping pass short.
 * - **A cap.** At most 10 000 objects are visited per estimate (`maxObjects`); each further one is charged
 *   a flat header and two references, without looking inside it.
 * - **No stack, no throw.** The walk is iterative, so a deep graph cannot overflow the stack, and a field
 *   that cannot be read is charged flat; an estimate never throws.
 *
 * Measured on the JVM against an exact walk (2026-11-14), estimate / exact, with the default pick
 * (the stride's figure in brackets where it differs):
 * - 6 400 strings, 10 % of them 1 000 chars long: at the front 0.95 (1.07), at the back 0.95; every
 *   10th 1.31 (7.8), every 7th 0.90 (1.08), every 64th 1.32 (1.97), every 100th 1.13 (**24.1**: a
 *   period that divides the stride is seen as all large). The golden ratio has its own worst case, a
 *   layout with the large elements exactly on its positions: 24.1 (1.13) - unlikely by accident.
 * - one 10 000-char string shared by all 6 400 elements of a list: **7.6** - the sampled elements reach
 *   it with the weight of all elements.
 * - a tree of 29 524 data-class nodes, beyond the cap: **0.51**.
 * - a set of 5 000 strings whose large ones are its last 500, beyond the sampling window: **0.24**.
 * - a list of 10 000 strings, a list of 1 000 maps of 10 entries, a map of 5 000 entries: 0.97 to 1.00.
 *
 * Cost of one estimate (list of 10 000 strings / 1 000 maps of 10 entries): JVM 0.08 ms / 1.9 ms,
 * JS 0.2 ms / 3.4 ms, native 0.7 ms / 3.9 ms - before sampling and the cap 17 ms / 70 ms, 450 ms /
 * 537 ms, 3.7 s / 16 s. 100 000 strings: JVM 0.09 ms (1.5 s before).
 *
 * ## Per platform
 *
 * - **Native: no per-object detail.** Without reflection, every custom object is charged a flat 32 bytes
 *   (header and two references); collections, maps, arrays and strings are walked as everywhere.
 * - **JS:** every number is one JS `number` and is charged 8 bytes. Strings have no identity, so two
 *   equal strings are counted once.
 * - **JVM: collections and maps are under-counted.** A map entry costs two references here, about 48
 *   bytes really (table, slack, entry object); boxed elements are charged their raw size.
 * - **JVM:** an inner class is charged without its outer instance; everything else it reaches is
 *   charged, captured locals and the delegate of class delegation included. Types whose fields JPMS
 *   does not open (`Instant`, `UUID`, `BigDecimal`, `StringBuilder`, ...) are charged an empty object.
 *
 * ## Your own estimator
 *
 * Where the number has to mean something, plug in an estimator that knows the values:
 *
 * ```kotlin
 * val cache = fastCache<String, Profile> {
 *     maxMemoryUsage(64L * 1024 * 1024, estimator = object : ObjectSizeEstimator {
 *         override fun estimate(obj: Any?): Long = when (obj) {
 *             is String -> 40L + 2L * obj.length
 *             is Profile -> 120L + 2L * obj.bio.length
 *             else -> 16L
 *         }
 *     })
 * }
 * ```
 *
 * Or tune the default with an [EstimatorConfig] (sample size, window, cap, header sizes).
 */
interface ObjectSizeEstimator {
    /** Factory for the default implementation. */
    companion object {
        /** Creates a default [ObjectSizeEstimatorImpl] with the given [cfg]. */
        operator fun invoke(cfg: EstimatorConfig = EstimatorConfig()): ObjectSizeEstimator {
            return ObjectSizeEstimatorImpl(cfg)
        }
    }

    /** Returns the estimated size of [obj] in bytes. Called on the cache's maintenance loop: keep it cheap. */
    fun estimate(obj: Any?): Long
}

/**
 * Default [ObjectSizeEstimator] implementation.
 *
 * Walks the object graph using platform-specific reflection (see [ObjectSizeEstimatorPlatform]) and
 * sums up estimated sizes for primitives, strings, arrays, collections, maps and arbitrary objects.
 *
 * The walk is bounded, so its cost is too - see [ObjectSizeEstimator] for what that costs in accuracy:
 * - an identity set of visited objects (constant time per object) keeps a shared object from being
 *   charged twice; it never calls `hashCode()` or `equals()` of a visited object;
 * - a collection, map or array with more than [EstimatorConfig.sampleSize] elements is sampled, and
 *   the rest is charged by the sampled elements' average;
 * - at most [EstimatorConfig.maxObjects] objects are visited per estimate; any further object is
 *   charged a flat header and two references, without descent;
 * - the walk is iterative, so a deep graph cannot overflow the stack, and an estimate never throws.
 *
 * Each call starts afresh, so one estimate never depends on another. The default [EstimatorConfig]
 * describes a 64-bit HotSpot heap and is used unchanged on JS and native.
 *
 * @param cfg tuning knobs for header and pointer sizes, sampling and the cap
 */
class ObjectSizeEstimatorImpl(
    /** Configuration for header, array, and pointer size heuristics, sampling and the cap. */
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
     * @property sampleSize   elements walked per collection, map or array; above it the rest is
     *   charged by the average of these, spread evenly over the elements
     * @property sampleWindow how many elements of a collection or map without index access (a set, a
     *   linked list, a map) the samples are spread over: the first ones, so the stepping pass stays short
     * @property maxObjects   objects visited per estimate; any further one is charged a flat header and
     *   two references, without descent
     * @property samplePick   where the samples sit - see [SamplePick]
     */
    data class EstimatorConfig(
        val objectHeader: Long = 16L,
        val arrayHeader: Long = 24L,
        val pointerSize: Long = 8L,
        val sampleSize: Int = 64,
        val sampleWindow: Int = 1_024,
        val maxObjects: Int = 10_000,
        val samplePick: SamplePick = SamplePick.GoldenRatio,
    )

    /**
     * Where the samples of a large collection sit. Both are deterministic: the same size always gives
     * the same positions, on every platform.
     */
    enum class SamplePick {
        /**
         * The default: positions `frac(i * 0.618... + 0.5) * size`, a low-discrepancy sequence. Spread
         * evenly without a fixed period, so a periodic layout is not missed or hit wholesale. Weak only
         * at a layout that happens to put the large elements on exactly these positions.
         */
        GoldenRatio,

        /**
         * Positions `i * size / sampleSize`, a fixed stride. Weak at a periodic layout whose period
         * divides the stride: every tenth element large, with a stride of 100, is seen as all large.
         */
        EvenlySpaced,
    }

    /** An object still to visit, and how many times its size counts (more than once when sampled). */
    private class Pending(val obj: Any?, val weight: Double)

    /** Estimates the in-memory size of [obj] in bytes. Never throws: a failed walk is charged flat. */
    override fun estimate(obj: Any?): Long = try {
        walk(obj)
    } catch (e: Throwable) {
        flatObject()
    }

    /** What an object is charged when it cannot be walked: a header and two references. */
    private fun flatObject(): Long = cfg.objectHeader + 2L * cfg.pointerSize

    /** The size of a primitive (or null), or null for anything else. */
    private fun primitiveSize(obj: Any?): Long? = when (obj) {
        null -> NULL_SIZE
        is Boolean -> BOOL_SIZE
        // Double first: on JS every number is one JS `number` and matches every numeric check, so it
        // must hit the widest; on the JVM the checks are exact and the order does not matter.
        is Double -> DOUBLE_SIZE
        is Float -> FLOAT_SIZE
        is Long -> LONG_SIZE
        is Int -> INT_SIZE
        is Short -> SHORT_SIZE
        is Byte -> BYTE_SIZE
        is Char -> CHAR_SIZE
        else -> null
    }

    private fun walk(root: Any?): Long {
        primitiveSize(root)?.let { return it }

        val seen = IdentitySet()
        val stack = ArrayList<Pending>()
        var total = 0.0
        var visited = 0

        stack.add(Pending(root, 1.0))

        // Iterative, with an explicit stack: a recursive walk overflowed the stack on a deep graph
        while (stack.isNotEmpty()) {
            val next = stack.removeAt(stack.lastIndex)
            val obj = next.obj
            val weight = next.weight

            val primitive = primitiveSize(obj)

            when {
                primitive != null -> total += primitive * weight

                obj == null -> Unit

                obj === UnreadableField -> total += flatObject() * weight

                // already counted: a shared object is charged once
                !seen.add(obj) -> Unit

                visited >= cfg.maxObjects -> total += flatObject() * weight

                else -> {
                    visited++
                    total += shallowSize(obj, weight, stack) * weight
                }
            }
        }

        return total.roundToLong()
    }

    /** The size of [obj] itself; pushes what it references onto [stack], sampled where large. */
    private fun shallowSize(obj: Any, weight: Double, stack: MutableList<Pending>): Long = when (obj) {
        is String -> cfg.objectHeader + cfg.pointerSize + cfg.arrayHeader + obj.length.toLong() * CHAR_SIZE

        is BooleanArray -> cfg.arrayHeader + BOOL_SIZE * obj.size
        is ByteArray -> cfg.arrayHeader + BYTE_SIZE * obj.size
        is ShortArray -> cfg.arrayHeader + SHORT_SIZE * obj.size
        is CharArray -> cfg.arrayHeader + CHAR_SIZE * obj.size
        is IntArray -> cfg.arrayHeader + INT_SIZE * obj.size
        is LongArray -> cfg.arrayHeader + LONG_SIZE * obj.size
        is FloatArray -> cfg.arrayHeader + FLOAT_SIZE * obj.size
        is DoubleArray -> cfg.arrayHeader + DOUBLE_SIZE * obj.size

        is Array<*> -> {
            pushIndexed(obj.size, weight, stack) { obj[it] }
            cfg.arrayHeader + obj.size.toLong() * cfg.pointerSize
        }

        is Collection<*> -> {
            if (obj is List<*> && obj is RandomAccess) {
                pushIndexed(obj.size, weight, stack) { obj[it] }
            } else {
                pushIterated(obj.size, weight, stack, obj.iterator()) { element, w -> stack.add(Pending(element, w)) }
            }
            cfg.objectHeader + obj.size.toLong() * cfg.pointerSize
        }

        is Map<*, *> -> {
            pushIterated(obj.size, weight, stack, obj.entries.iterator()) { entry, w ->
                stack.add(Pending(entry.key, w))
                stack.add(Pending(entry.value, w))
            }
            cfg.objectHeader + obj.size.toLong() * 2 * cfg.pointerSize
        }

        else -> {
            val fields = ObjectSizeEstimatorPlatform.getFieldsOf(obj)

            if (fields == null) {
                // No reflection (native): a flat guess of a header and two references, whatever the
                // object holds - see the class KDoc.
                flatObject()
            } else {
                fields.forEach { stack.add(Pending(it, weight)) }
                cfg.objectHeader + fields.size.toLong() * cfg.pointerSize
            }
        }
    }

    /**
     * Pushes the elements of an indexed list or array: all of them up to [EstimatorConfig.sampleSize],
     * else that many at the positions [EstimatorConfig.samplePick] gives, each counting for its share of
     * the rest. Deterministic: no random pick, so the same value always gives the same estimate.
     */
    private fun pushIndexed(size: Int, weight: Double, stack: MutableList<Pending>, elementAt: (Int) -> Any?) {
        if (size <= cfg.sampleSize) {
            for (i in 0 until size) stack.add(Pending(elementAt(i), weight))
            return
        }

        val positions = samplePositions(size, cfg.sampleSize, cfg.samplePick)
        val share = weight * size / positions.size

        positions.forEach { stack.add(Pending(elementAt(it), share)) }
    }

    /**
     * Pushes the elements of a collection or map without index access: all of them up to
     * [EstimatorConfig.sampleSize], else that many at the positions [EstimatorConfig.samplePick] gives
     * over the first [EstimatorConfig.sampleWindow] - one stepping pass, kept short.
     */
    private fun <T> pushIterated(
        size: Int,
        weight: Double,
        stack: MutableList<Pending>,
        elements: Iterator<T>,
        push: (T, Double) -> Unit,
    ) {
        val samples = cfg.sampleSize

        if (size <= samples) {
            elements.forEach { push(it, weight) }
            return
        }

        val window = minOf(size, maxOf(cfg.sampleWindow, samples))
        // The same positions as pushIndexed, over the window, ascending. (A fixed step of
        // window / samples rounded down once left the window's tail unsampled.)
        val positions = samplePositions(window, samples, cfg.samplePick)
        val share = weight * size / positions.size
        var index = 0
        var taken = 0

        while (taken < positions.size && elements.hasNext()) {
            val element = elements.next()

            if (index == positions[taken]) {
                push(element, share)
                taken++
            }

            index++
        }
    }
}

/** The golden ratio's fractional part, for [ObjectSizeEstimatorImpl.SamplePick.GoldenRatio]. */
private const val GOLDEN_FRACTION = 0.6180339887498949

/**
 * The distinct positions to sample from `0 until size`, ascending: all of them when [size] is at most
 * [samples], else up to [samples] picked by [pick]. Never [size] or above.
 */
internal fun samplePositions(size: Int, samples: Int, pick: ObjectSizeEstimatorImpl.SamplePick): IntArray {
    if (size <= samples) {
        return IntArray(size) { it }
    }

    return when (pick) {
        ObjectSizeEstimatorImpl.SamplePick.EvenlySpaced ->
            IntArray(samples) { i -> (i.toLong() * size / samples).toInt() }

        ObjectSizeEstimatorImpl.SamplePick.GoldenRatio ->
            IntArray(samples) { i ->
                val x = i * GOLDEN_FRACTION + 0.5
                val fraction = x - floor(x)
                // fraction < 1, so the product is below size; the clamp guards floating point rounding
                minOf((fraction * size).toLong(), size - 1L).toInt()
            }.distinct().sorted().toIntArray()
    }
}

/** Stands in for a field or property whose read failed: charged flat, never descended into. */
internal object UnreadableField
