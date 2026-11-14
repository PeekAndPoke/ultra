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

            (ObjectSizeEstimator().estimate(map) > 0L) shouldBe true
            (ObjectSizeEstimator().estimate(holder) > 0L) shouldBe true
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
            var nested: Any = emptyList<Any>()
            repeat(100_000) { nested = listOf(nested) }

            (ObjectSizeEstimator().estimate(nested) > 0L) shouldBe true
        }

        "beyond the cap, objects are charged flat without descent" {
            val estimator = ObjectSizeEstimator(ObjectSizeEstimatorImpl.EstimatorConfig(maxObjects = 100))

            var nested: Any = emptyList<Any>()
            repeat(1_000) { nested = listOf(nested) }

            // 100 lists of one element visited (16 + 8 each), then the 101st object charged flat
            // (header and two references), and nothing below it
            estimator.estimate(nested) shouldBe 100 * (16L + 8L) + (16L + 2 * 8L)
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
