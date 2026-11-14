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

        "the same value always gives the same estimate" {
            val (list, _) = mixed(6_400) { it % 7 == 0 }

            ObjectSizeEstimator().estimate(list) shouldBe ObjectSizeEstimator().estimate(list)
        }
    }
}
