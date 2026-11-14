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
 * - **A cap.** At most 10 000 objects are visited per estimate (`maxObjects`), breadth first, so the cap
 *   cuts the deepest levels and never a whole sibling subtree. A string or primitive array is always
 *   sized from its length: it needs no descent and does not count against the cap. Beyond the cap,
 *   any other object is charged a flat header and two references, without looking inside it - **so
 *   the cap errs low**, by what the cut objects hold.
 * - **A bounded identity set.** A shared object is charged once while the set holds fewer than twice
 *   `maxObjects` objects; beyond that every object counts as new, so a shared one may count again -
 *   that errs high.
 * - **No stack, no throw.** The walk is iterative, so a deep graph cannot overflow the stack, and a field
 *   that cannot be read is charged flat; an estimate never throws.
 *
 * The work is bounded by the configuration, not by the value: at most `maxObjects` visits, each
 * handing on at most `sampleSize` elements (twice that for a map) or the fields of one object, each
 * sized or queued in constant time. Worked example: a list of 100 lists of 100 lists of 100 strings
 * (a million strings, 74 MB). Sampling walks 64 of each 100, so 1 + 64 + 4 096 lists are visited and
 * 262 144 strings sized - estimate / exact 1.00, 16 ms on the JVM. Rule of thumb for when the cap
 * cuts: a value whose top level holds `n` elements of `k` objects each (strings and primitive arrays
 * not counted) stays below the cap while `min(n, 64) * k` stays below `maxObjects`.
 *
 * Measured on the JVM against an exact walk (2026-11-14), estimate / exact, with the default pick
 * (the stride's figure in brackets where it differs):
 * - 6 400 strings, 10 % of them 1 000 chars long: at the front 0.95 (1.07), at the back 0.95; every
 *   10th 1.31 (7.8), every 7th 0.90 (1.08), every 64th 1.32 (1.97), every 100th 1.13 (**24.1**: a
 *   period that divides the stride is seen as all large). The golden ratio has its own worst case, a
 *   layout with the large elements exactly on its positions: 24.1 (1.13) - unlikely by accident.
 * - 6 400 `Holder(shared, id)` objects, all holding one 10 000-char string: **7.6** - the sampled
 *   holders reach the string with the weight of all of them. (A plain list of 6 400 references to
 *   the string: 28.9.)
 * - 100 orders of 100 lines of 15 objects each, 150 000 objects, far beyond the cap: **0.63**; of 5
 *   objects each, below it: 1.00.
 * - a 50 MB byte array beside a table of 40 000 cells, a 10 MB one beside a tree of 9 841 nodes, a
 *   list of 10 000 strings, a list of 1 000 maps of 10 entries: 1.00; a map of 5 000 entries: 0.97.
 * - a set of 5 000 strings whose large ones are its last 500, beyond the sampling window: **0.24**.
 *
 * Cost of one estimate on the JVM: a list of 10 000 strings 0.03 ms, 100 000 strings 0.1 ms, 1 000
 * maps of 10 entries 0.2 ms (before sampling and the cap: 17 ms, 1.5 s and 450 ms).
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
 * - an identity set (constant time per object, at most twice [EstimatorConfig.maxObjects] objects)
 *   keeps a shared object from being charged twice; it never calls `hashCode()` or `equals()`;
 * - a collection, map or array with more than [EstimatorConfig.sampleSize] elements is sampled, and
 *   the rest is charged by the sampled elements' average;
 * - at most [EstimatorConfig.maxObjects] objects are visited per estimate, breadth first; strings and
 *   primitive arrays are sized from their length and never count; any further object is charged a
 *   flat header and two references, without descent;
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
     * @property maxObjects   objects visited per estimate, strings and primitive arrays not counted; any
     *   further one is charged a flat header and two references, without descent. The identity set
     *   holds at most twice as many
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
    private class Pending(val obj: Any, val weight: Double)

    /** Estimates the in-memory size of [obj] in bytes. Never throws: a failed walk is charged flat. */
    override fun estimate(obj: Any?): Long = try {
        Walk().run(obj)
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

    /**
     * One estimate: breadth-first, so the cap cuts the deepest level and never whole sibling subtrees.
     *
     * The cap counts the objects taken off the queue. Once objects visited plus queued reach it,
     * nothing more is queued: a string or primitive array is sized on the spot from its length (it
     * needs no descent), anything else is charged flat. So the queue never holds more than
     * [EstimatorConfig.maxObjects] entries, and the work is at most that many visits plus one
     * constant-time sizing per reference they hold.
     */
    private inner class Walk {
        private val seen = IdentitySet()
        private val queue = ArrayDeque<Pending>()
        private var total = 0.0
        private var visited = 0
        private var tracked = 0

        fun run(root: Any?): Long {
            primitiveSize(root)?.let { return it }

            push(root, 1.0)

            // Iterative: a recursive walk overflowed the stack on a deep graph
            while (queue.isNotEmpty()) {
                visit(queue.removeFirst())
            }

            return total.roundToLong()
        }

        /**
         * Charges [obj] now, or queues it: a primitive, an unreadable field, a string or primitive array
         * (sized from its length, no descent) and anything beyond the cap are charged now.
         */
        fun push(obj: Any?, weight: Double) {
            val primitive = primitiveSize(obj)

            when {
                primitive != null -> total += primitive * weight

                obj === UnreadableField -> total += flatObject() * weight

                // a shared object is charged once - while the identity set has room, see below
                !firstSighting(obj!!) -> Unit

                else -> {
                    // A leaf never counts against the cap: charged flat beyond it, a 50 MB array behind
                    // a large sibling once counted as 32 bytes.
                    val leaf = leafSize(obj)

                    when {
                        leaf != null -> total += leaf * weight
                        // beyond the cap: no descent
                        visited + queue.size >= cfg.maxObjects -> total += flatObject() * weight
                        else -> queue.addLast(Pending(obj, weight))
                    }
                }
            }
        }

        /**
         * True unless [obj] was seen before. The identity set takes at most twice
         * [EstimatorConfig.maxObjects] objects; beyond that, every object counts as new. That bounds
         * the hashing, which dominated the time on a million-string value; it errs high, on an object
         * shared beyond that point.
         */
        private fun firstSighting(obj: Any): Boolean {
            if (tracked >= 2L * cfg.maxObjects) return true

            return seen.add(obj).also { if (it) tracked++ }
        }

        private fun visit(next: Pending) {
            visited++
            // Not `total += shallowSize(...)`: that reads total before shallowSize charges the leaves it
            // pushes, and the write would drop them.
            val own = shallowSize(next.obj, next.weight, this)
            total += own * next.weight
        }
    }

    /** The size of a string or primitive array, which needs no descent, or null for anything else. */
    private fun leafSize(obj: Any): Long? = when (obj) {
        is String -> cfg.objectHeader + cfg.pointerSize + cfg.arrayHeader + obj.length.toLong() * CHAR_SIZE

        is BooleanArray -> cfg.arrayHeader + BOOL_SIZE * obj.size
        is ByteArray -> cfg.arrayHeader + BYTE_SIZE * obj.size
        is ShortArray -> cfg.arrayHeader + SHORT_SIZE * obj.size
        is CharArray -> cfg.arrayHeader + CHAR_SIZE * obj.size
        is IntArray -> cfg.arrayHeader + INT_SIZE * obj.size
        is LongArray -> cfg.arrayHeader + LONG_SIZE * obj.size
        is FloatArray -> cfg.arrayHeader + FLOAT_SIZE * obj.size
        is DoubleArray -> cfg.arrayHeader + DOUBLE_SIZE * obj.size

        else -> null
    }

    /** The size of [obj] itself; pushes what it references to [walk], sampled where large. */
    private fun shallowSize(obj: Any, weight: Double, walk: Walk): Long = when (obj) {
        is Array<*> -> {
            pushIndexed(obj.size, weight, walk) { obj[it] }
            cfg.arrayHeader + obj.size.toLong() * cfg.pointerSize
        }

        is Collection<*> -> {
            if (obj is List<*> && obj is RandomAccess) {
                pushIndexed(obj.size, weight, walk) { obj[it] }
            } else {
                pushIterated(obj.size, weight, walk, obj.iterator()) { element, w -> walk.push(element, w) }
            }
            cfg.objectHeader + obj.size.toLong() * cfg.pointerSize
        }

        is Map<*, *> -> {
            pushIterated(obj.size, weight, walk, obj.entries.iterator()) { entry, w ->
                walk.push(entry.key, w)
                walk.push(entry.value, w)
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
                fields.forEach { walk.push(it, weight) }
                cfg.objectHeader + fields.size.toLong() * cfg.pointerSize
            }
        }
    }

    /**
     * Pushes the elements of an indexed list or array: all of them up to [EstimatorConfig.sampleSize],
     * else that many at the positions [EstimatorConfig.samplePick] gives, each counting for its share of
     * the rest. Deterministic: no random pick, so the same value always gives the same estimate.
     */
    private fun pushIndexed(size: Int, weight: Double, walk: Walk, elementAt: (Int) -> Any?) {
        if (size <= cfg.sampleSize) {
            for (i in 0 until size) walk.push(elementAt(i), weight)
            return
        }

        val positions = samplePositions(size, cfg.sampleSize, cfg.samplePick)
        val share = weight * size / positions.size

        positions.forEach { walk.push(elementAt(it), share) }
    }

    /**
     * Pushes the elements of a collection or map without index access: all of them up to
     * [EstimatorConfig.sampleSize], else that many at the positions [EstimatorConfig.samplePick] gives
     * over the first [EstimatorConfig.sampleWindow] - one stepping pass, kept short.
     */
    private fun <T> pushIterated(
        size: Int,
        weight: Double,
        walk: Walk,
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
