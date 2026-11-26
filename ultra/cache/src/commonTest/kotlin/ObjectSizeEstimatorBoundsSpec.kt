package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeBetween
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * The default estimator's bounded cost: identity tracking, sampling spread over a collection, a cap
 * on the objects visited, an iterative walk.
 */
class ObjectSizeEstimatorBoundsSpec : StringSpec() {

    /** What the estimator charges a string of [length] chars, by its own formula. */
    private fun stringCost(length: Int): Long = 16L + 8L + 24L + 2L * length

    /**
     * A list of [n] distinct strings, about 1 000 chars long at the indexes [isLarge] picks, short
     * elsewhere. Distinct: a shared literal would be counted once, and a set would keep only one.
     */
    private fun mixed(n: Int, isLarge: (Int) -> Boolean): Pair<List<String>, Long> {
        val list = List(n) { i -> if (isLarge(i)) "L" + "x".repeat(999) + i else "s$i" }
        // a list: header and one reference per element, then every element
        val exact = 16L + 8L * n + list.sumOf { stringCost(it.length) }

        return list to exact
    }

    init {
        timeout = TEST_TIMEOUT_MS

        "a map containing itself, and a cycle through a list, still terminate" {
            val map = mutableMapOf<String, Any>()
            map["self"] = map

            val holder = mutableListOf<Any>()
            val node = mutableMapOf<String, Any>("list" to holder)
            holder.add(node)

            // the map (16 + 2 * 8) and its key "self" (16 + 8 + 24 + 8); reached again, it counts nothing
            ObjectSizeEstimator().estimate(map) shouldBe 88L
            // the list (16 + 8), the map (32), its key "list" (56)
            ObjectSizeEstimator().estimate(holder) shouldBe 112L
        }

        "objects whose hashCode and equals throw are estimated, each counted" {
            class Hostile {
                override fun hashCode(): Int = error("hashCode must not be called")

                override fun equals(other: Any?): Boolean = error("equals must not be called")
            }

            val one = ObjectSizeEstimator().estimate(listOf(Hostile()))
            val two = ObjectSizeEstimator().estimate(listOf(Hostile(), Hostile()))

            two shouldBeGreaterThan one
        }

        "a 100 000-deep nesting does not overflow the stack" {
            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 200_000))
            var nested: Any = emptyList<Any>()
            repeat(100_000) { nested = listOf(nested) }

            // every list walked, none charged flat: 16 + 8 each, and the empty one at the bottom
            estimator.estimate(nested) shouldBe 100_000 * 24L + 16L
        }

        "an object that fails to walk is charged flat; the rest of the estimate stands" {
            val estimate = ObjectSizeEstimator().estimate(listOf(ByteArray(50_000_000), Boom()))

            // the list (16 + 2 * 8), the array (24 + 50 000 000), Boom flat (16 + 2 * 8)
            estimate shouldBe 32L + 50_000_024L + 32L
        }

        "failures are counted, with the class of the first one" {
            val counted = ObjectSizeEstimatorImpl().estimateCounted(listOf(Boom(), "ok", Boom()))

            // the list (16 + 3 * 8), each Boom flat (32), "ok" (52)
            counted.bytes shouldBe 40L + 32L + 52L + 32L
            counted.failures shouldBe 2
            counted.failureClass shouldBe "IllegalStateException"

            ObjectSizeEstimatorImpl().estimateCounted(listOf("ok")).failures shouldBe 0
        }

        "a sample size below one counts as one" {
            fun estimateWith(sampleSize: Int) = ObjectSizeEstimator(
                ObjectSizeEstimatorImpl.EstimatorConfig(sampleSize = sampleSize)
            ).estimate(List(100) { "s$it" })

            val one = estimateWith(1)

            // the list (16 + 100 * 8) and more: the one sample counts for all 100 strings
            (one > 816L + 100 * 50L) shouldBe true
            estimateWith(0) shouldBe one
            estimateWith(-5) shouldBe one
        }

        "a sampling window below the sample size counts as the sample size" {
            val (list, _) = mixed(1_000) { it % 10 == 0 }
            val set = LinkedHashSet(list)

            fun estimateWith(window: Int) = ObjectSizeEstimator(
                ObjectSizeEstimatorImpl.EstimatorConfig(sampleWindow = window)
            ).estimate(set)

            estimateWith(0) shouldBe estimateWith(64)
            estimateWith(-1) shouldBe estimateWith(64)
        }

        "a cap below one counts as one" {
            fun estimateWith(maxObjects: Int) = ObjectSizeEstimator(
                ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = maxObjects)
            ).estimate(listOf("a", "b"))

            // the list walked (16 + 2 * 8), its strings sized (50 each)
            estimateWith(1) shouldBe 132L
            estimateWith(0) shouldBe 132L
            estimateWith(-1) shouldBe 132L
        }

        "a collection is read for its size once, and iterated no further than that size" {
            val counting = CountingCollection(claimed = 3, length = 10_000)

            // the collection (16 + 3 * 8), "s" once: a shared string counts once
            ObjectSizeEstimator().estimate(counting) shouldBe 40L + 50L
            counting.sizeReads shouldBe 1
            counting.nexts shouldBe 3
        }

        "a large collection without index access is stepped through its sampling window only" {
            val counting = CountingCollection(claimed = 1_000_000, length = 1_000_000)

            ObjectSizeEstimator().estimate(counting)

            (counting.nexts <= 1_024) shouldBe true
        }

        "a small map is exact, keys and values" {
            // the map (16 + 2 * 2 * 8), keys "a" and "b" (50 each), values "xy" (52) and "z" (50)
            ObjectSizeEstimator().estimate(mapOf("a" to "xy", "b" to "z")) shouldBe 48L + 50L + 52L + 50L + 50L
        }

        "a sampled map charges keys and values" {
            val map = (0 until 1_000).associate { "k$it" to "v$it" }
            val exact = 16L + 1_000 * 16L +
                map.entries.sumOf { stringCost(it.key.length) + stringCost(it.value.length) }

            (ObjectSizeEstimator().estimate(map).toDouble() / exact).shouldBeBetween(0.95, 1.05, 0.0)
        }

        "an array of strings is exact" {
            // the array (24 + 2 * 8), "ab" (52), "cde" (54)
            ObjectSizeEstimator().estimate(arrayOf("ab", "cde")) shouldBe 40L + 52L + 54L
        }

        "beyond the cap, a sampled element is charged flat for its whole share" {
            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 1))

            // the list (16 + 1 000 * 8) walked; its sampled elements, beyond the cap, charged flat for
            // all 1 000 of them
            estimator.estimate(List(1_000) { listOf(it) }) shouldBe 8_016L + 1_000 * 32L
        }

        "beyond the cap, objects are charged flat without descent" {
            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 100))

            var nested: Any = emptyList<Any>()
            repeat(1_000) { nested = listOf(nested) }

            // 100 lists of one element visited (16 + 8 each), then the 101st object charged flat
            // (header and two references), and nothing below it
            estimator.estimate(nested) shouldBe 100 * (16L + 8L) + (16L + 2 * 8L)
        }

        "the cap: a string or primitive array behind a large sibling is sized, not charged flat" {
            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 100))
            // 241 lists, more than the cap
            val table = List(60) { r -> List(3) { c -> listOf("s$r-$c") } }
            val blob = ByteArray(1_000_000)
            // two lists deep, so in the first order the blob is reached only once the table used up the cap
            val behind = listOf(listOf(blob))

            (estimator.estimate(listOf(table, behind)) >= 1_000_024L) shouldBe true
            (estimator.estimate(listOf(behind, table)) >= 1_000_024L) shouldBe true
            (estimator.estimate(listOf(blob, table)) >= 1_000_024L) shouldBe true
            (estimator.estimate(listOf(table, blob)) >= 1_000_024L) shouldBe true
        }

        "the cap: strings never count against it - 111 lists holding 1 000 strings are exact under a cap of 200" {
            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 200))
            val value = List(10) { a -> List(10) { b -> List(10) { c -> "s$a$b$c" } } }

            // 111 lists of 10 (16 + 10 * 8 each), 1 000 distinct strings of 4 chars (16 + 8 + 24 + 8 each)
            estimator.estimate(value) shouldBe 111 * 96L + 1_000 * 56L
        }

        "the cap: breadth first - a deep sibling cannot use it up before a shallow one is reached" {
            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 100))
            var chain: Any = emptyList<Any>()
            repeat(1_000) { chain = listOf(chain) }
            // two lists deep, behind no leaf of its own: a depth-first walk reached it after the chain
            val shallow = listOf(listOf("x".repeat(100_000)))

            (estimator.estimate(listOf(shallow, chain)) >= 200_048L) shouldBe true
            (estimator.estimate(listOf(chain, shallow)) >= 200_048L) shouldBe true
        }

        "the identity set is bounded: past the cap in strings, a shared string counts again" {
            val shared = "x".repeat(1_000)
            val value = List(30) { "s$it" } + List(5) { shared }
            // the list (16 + 35 * 8), 10 strings of 2 chars and 20 of 3 (52 and 54), the shared one (2 048)
            val once = 296L + 10 * 52L + 20 * 54L + 2_048L

            ObjectSizeEstimator().estimate(value) shouldBe once
            // the first 10 strings are tracked; every later one is sized without the check, shared too
            ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 10)).estimate(value) shouldBe
                once + 4 * 2_048L
        }

        "the identity set is bounded, yet a back-reference never walks a cycle again" {
            val parent = ArrayList<Any>()
            val child = listOf("a", "b", "c", "d", "e", "f", "g", parent)
            repeat(80) { parent.add(child) }

            val estimator = ObjectSizeEstimator(
                ObjectSizeEstimatorImpl.EstimatorConfig(sampleSize = 8, maxObjects = 4)
            )

            // the parent (16 + 80 * 8); the child, sampled 8 of 80 times, so with the weight 10: itself
            // (16 + 8 * 8) and its seven strings (50 each); the parent behind it already counted
            estimator.estimate(parent) shouldBe 656L + 10 * (80L + 7 * 50L)
        }

        "the identity set is bounded, yet a doubly linked chain is walked once" {
            fun node(vararg strings: String) = mutableListOf<Any?>(strings[0], strings[1], strings[2], null, null)

            val nodes = List(4) { i -> node("a$i", "b$i", "c$i") } +
                List(2) { i -> node("x".repeat(999) + i, "y".repeat(999) + i, "z".repeat(999) + i) }
            nodes.zipWithNext { a, b ->
                a[4] = b
                b[3] = a
            }

            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 6))

            // six nodes of five references (56 each), twelve 2-char strings (52), six 1 000-char ones
            // (2 048), the two nulls at the ends (4 each)
            estimator.estimate(nodes[0]) shouldBe 6 * 56L + 12 * 52L + 6 * 2_048L + 2 * 4L
        }

        "with the default settings, parents and children referring to each other are counted once" {
            val value = List(64) { p ->
                val parent = ArrayList<Any>()
                val children = ArrayList<Any>()
                parent.add(children)
                repeat(640) { c -> children.add(listOf("a$p-$c", "b$p-$c", "c$p-$c", "d$p-$c", "e$p-$c", parent)) }
                parent
            }

            // the same estimate as without any budget (round 2: 15 515 408): every object is walked once
            ObjectSizeEstimator().estimate(value) shouldBe 15_515_408L
        }

        "the cap counts the queued objects too, so it cuts before they are walked" {
            val value = List(4) { a -> List(4) { b -> listOf("s$a$b") } }
            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 10))

            // walked: the outer list (16 + 4 * 8), the four inner ones (48 each), five of the sixteen
            // innermost (16 + 8 and a 3-char string of 54); the other eleven charged flat (32)
            estimator.estimate(value) shouldBe 48L + 4 * 48L + 5 * (24L + 54L) + 11 * 32L
        }

        "sampling: a sampled element of a list weighs the elements over the distinct positions" {
            // 65 elements give 55 distinct positions: each sampled string counts 65 / 55 times
            val list = List(65) { "s" + (100 + it) }

            // the list (16 + 65 * 8) and 65 strings of 4 chars (56 each)
            ObjectSizeEstimator().estimate(list) shouldBe 536L + 65 * 56L
        }

        "sampling: a sampled element of a set weighs the elements over the distinct positions" {
            val set = LinkedHashSet(List(65) { "s" + (100 + it) })

            // the set (16 + 65 * 8) and 65 strings of 4 chars (56 each), as for the list
            ObjectSizeEstimator().estimate(set) shouldBe 536L + 65 * 56L
        }

        "a weight past what a Double holds saturates at Long.MAX_VALUE, it does not fail" {
            // 300 levels of 1 000 elements, the next level in the middle (always sampled): each level
            // multiplies the weight by 1 000 / 64, past Double.MAX_VALUE; the zeros are charged with it
            fun level(next: Any) = List(1_000) { if (it == 500) next else 0 }

            var value: Any = 0
            repeat(300) { value = level(value) }

            val counted = ObjectSizeEstimatorImpl().estimateCounted(value)

            counted.bytes shouldBe Long.MAX_VALUE
            counted.failures shouldBe 0
        }

        "a weight past what a Double holds with negative header sizes does not fail either" {
            // the lists grow past +Infinity; the strings, negative by their header, are charged 0, not
            // -Infinity (beside +Infinity a NaN). Strings, not objects: they never count against the
            // cap, so every level is walked
            fun level(depth: Int, next: Any) = List(1_000) { if (it == 500) next else "s$depth-$it" }

            var value: Any = "s"
            repeat(300) { depth -> value = level(depth, value) }

            val counted = ObjectSizeEstimatorImpl(ObjectSizeEstimatorImpl.EstimatorConfig(objectHeader = -100L))
                .estimateCounted(value)

            counted.bytes shouldBe Long.MAX_VALUE
            counted.failures shouldBe 0
        }

        "a collection or a map claiming a negative size fails as that part only, never a negative amount" {
            val counted = ObjectSizeEstimatorImpl().estimateCounted(listOf("ok", NegativeCollection(), NegativeMap()))

            // the list (16 + 3 * 8) and "ok" (52); the two that claim -1 000 000 000 elements flat (32)
            counted.bytes shouldBe 40L + 52L + 2 * 32L
            counted.failures shouldBe 2
            counted.failureClass shouldBe "IllegalStateException"
        }

        "sampling: large elements at the back of a list are seen" {
            val (list, exact) = mixed(6_400) { it >= 5_760 }

            (ObjectSizeEstimator().estimate(list).toDouble() / exact).shouldBeBetween(0.75, 1.25, 0.0)
        }

        "sampling: large elements at the front of a list are seen" {
            val (list, exact) = mixed(6_400) { it < 640 }

            (ObjectSizeEstimator().estimate(list).toDouble() / exact).shouldBeBetween(0.75, 1.25, 0.0)
        }

        "sampling: a set within the sampling window is sampled across its whole length" {
            val (list, exact) = mixed(1_000) { it >= 900 }
            val set = LinkedHashSet(list)
            // a set's header and references are charged like a list's; the strings are distinct
            (ObjectSizeEstimator().estimate(set).toDouble() / exact).shouldBeBetween(0.75, 1.25, 0.0)
        }

        "golden-ratio positions are the same on every platform, distinct and below the size" {
            val golden = ObjectSizeEstimatorImpl.SamplePick.GoldenRatio

            samplePositions(6_400, 64, golden).toList() shouldBe listOf(
                42, 126, 262, 398, 482, 619, 703, 755, 839, 975, 1060, 1196, 1280, 1332, 1416, 1552,
                1637, 1773, 1909, 1993, 2130, 2214, 2266, 2350, 2486, 2570, 2707, 2791, 2843, 2927, 3063, 3147,
                3200, 3284, 3420, 3504, 3640, 3725, 3777, 3861, 3997, 4081, 4217, 4354, 4438, 4574, 4658, 4710,
                4795, 4931, 5015, 5151, 5235, 5287, 5372, 5508, 5592, 5728, 5865, 5949, 6085, 6169, 6221, 6305,
            )

            val large = samplePositions(1_000_003, 64, golden).toList()
            large.size shouldBe 64
            large.take(8) shouldBe listOf(6577, 19733, 41019, 62306, 75461, 96748, 109903, 118034)
            large.takeLast(4) shouldBe listOf(950852, 964008, 972138, 985294)

            val largest = samplePositions(Int.MAX_VALUE, 64, golden).toList()
            largest.take(4) shouldBe listOf(14125736, 42377210, 88089054, 133800898)
            (largest.last() < Int.MAX_VALUE) shouldBe true

            // just above the sample size, positions collide and are de-duplicated
            samplePositions(65, 64, golden).size shouldBe 55
        }

        "evenly spaced positions are a stride" {
            samplePositions(6_400, 64, ObjectSizeEstimatorImpl.SamplePick.EvenlySpaced).toList() shouldBe
                List(64) { it * 100 }
        }

        "the default pick: a periodic layout (every tenth element large) is neither missed nor hit wholesale" {
            val (list, exact) = mixed(6_400) { it % 10 == 0 }

            (ObjectSizeEstimator().estimate(list).toDouble() / exact).shouldBeBetween(0.75, 1.5, 0.0)
        }

        "the evenly spaced pick is honoured when configured - and sees every tenth element as all large" {
            val estimator = ObjectSizeEstimator(
                ObjectSizeEstimatorImpl.EstimatorConfig(samplePick = ObjectSizeEstimatorImpl.SamplePick.EvenlySpaced)
            )
            val (list, exact) = mixed(6_400) { it % 10 == 0 }

            // its stride of 100 lands on large elements only: the documented weakness of this pick
            (estimator.estimate(list).toDouble() / exact > 5.0) shouldBe true
        }

        "the evenly spaced pick is honoured in the stepping pass over a set as well" {
            val (list, exact) = mixed(6_400) { it % 10 == 0 }
            val set = LinkedHashSet(list)

            fun estimateWith(pick: ObjectSizeEstimatorImpl.SamplePick) = ObjectSizeEstimator(
                // the window opened to the whole set, so the pick alone decides what is seen
                ObjectSizeEstimatorImpl.EstimatorConfig(sampleWindow = 6_400, samplePick = pick)
            ).estimate(set).toDouble() / exact

            (estimateWith(ObjectSizeEstimatorImpl.SamplePick.EvenlySpaced) > 5.0) shouldBe true
            estimateWith(ObjectSizeEstimatorImpl.SamplePick.GoldenRatio).shouldBeBetween(0.75, 1.5, 0.0)
        }

        "the same value always gives the same estimate" {
            val (list, _) = mixed(6_400) { it % 7 == 0 }

            ObjectSizeEstimator().estimate(list) shouldBe ObjectSizeEstimator().estimate(list)
        }
    }
}

/** A list whose elements cannot be read: every access throws. */
private class Boom : AbstractList<String>() {
    override val size: Int = 3

    override fun get(index: Int): String = error("boom")
}

/** Claims [claimed] elements, yields [length] of them, and counts the reads of its size and elements. */
private class CountingCollection(private val claimed: Int, private val length: Int) : AbstractCollection<String>() {
    var sizeReads = 0
    var nexts = 0

    override val size: Int
        get() {
            sizeReads++
            return claimed
        }

    override fun iterator(): Iterator<String> = object : Iterator<String> {
        override fun hasNext(): Boolean = nexts < length

        override fun next(): String {
            nexts++
            return "s"
        }
    }
}

/** Claims a negative number of elements and has none. */
private class NegativeCollection : AbstractCollection<String>() {
    override val size: Int = -1_000_000_000

    override fun iterator(): Iterator<String> = emptyList<String>().iterator()
}

/** Claims a negative number of entries and has none. */
private class NegativeMap : AbstractMap<String, String>() {
    override val size: Int = -1_000_000_000

    override val entries: Set<Map.Entry<String, String>> = emptySet()
}
