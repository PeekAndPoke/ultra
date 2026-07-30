package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class ObjectSizeEstimatorImplSpec : StringSpec() {

    private val cfg = ObjectSizeEstimatorImpl.EstimatorConfig()

    init {
        "null estimates to NULL_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate(null) shouldBe ObjectSizeEstimatorImpl.NULL_SIZE
        }

        "Boolean estimates to BOOL_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate(true) shouldBe ObjectSizeEstimatorImpl.BOOL_SIZE
            estimator.estimate(false) shouldBe ObjectSizeEstimatorImpl.BOOL_SIZE
        }

        // NOTE: Individual numeric type tests (Short, Int, Float, Double, etc.) are in jvmTest only,
        // because Kotlin/JS represents all numbers as JS `number` — type checks like `is Byte`
        // match all numeric values, making type-specific size estimation impossible on JS.

        "String size includes objectHeader + pointerSize + arrayHeader + chars" {
            val estimator = ObjectSizeEstimator()
            val str = "hello"
            val expected = cfg.objectHeader + cfg.pointerSize + cfg.arrayHeader +
                    str.length.toLong() * ObjectSizeEstimatorImpl.CHAR_SIZE

            estimator.estimate(str) shouldBe expected
        }

        "empty String is smaller than non-empty String" {
            val estimator = ObjectSizeEstimator()

            val emptySize = estimator.estimate("")
            val nonEmptySize = estimator.estimate("abc")

            nonEmptySize shouldBeGreaterThan emptySize
        }

        "BooleanArray estimate is arrayHeader + BOOL_SIZE * length" {
            val estimator = ObjectSizeEstimator()
            val arr = booleanArrayOf(true, false, true)

            estimator.estimate(arr) shouldBe cfg.arrayHeader + 3 * ObjectSizeEstimatorImpl.BOOL_SIZE
        }

        "ByteArray estimate is arrayHeader + BYTE_SIZE * length" {
            val estimator = ObjectSizeEstimator()
            val arr = byteArrayOf(1, 2)

            estimator.estimate(arr) shouldBe cfg.arrayHeader + 2 * ObjectSizeEstimatorImpl.BYTE_SIZE
        }

        "empty Collection estimate is objectHeader only" {
            val estimator = ObjectSizeEstimator()
            val list = emptyList<Int>()

            estimator.estimate(list) shouldBe cfg.objectHeader
        }

        "Collection estimate is greater than objectHeader" {
            val estimator = ObjectSizeEstimator()
            val list = listOf(1, 2, 3)

            estimator.estimate(list) shouldBeGreaterThan cfg.objectHeader
        }

        "Map estimate is greater than objectHeader" {
            val estimator = ObjectSizeEstimator()
            val map = mapOf(1 to 2)

            estimator.estimate(map) shouldBeGreaterThan cfg.objectHeader
        }

        "custom EstimatorConfig changes calculation" {
            val customCfg = ObjectSizeEstimatorImpl.EstimatorConfig(
                objectHeader = 32L,
                arrayHeader = 48L,
                pointerSize = 4L,
            )
            val estimator = ObjectSizeEstimator(customCfg)

            val str = "ab"
            val expected = customCfg.objectHeader + customCfg.pointerSize + customCfg.arrayHeader +
                    2L * ObjectSizeEstimatorImpl.CHAR_SIZE

            estimator.estimate(str) shouldBe expected
        }

        // Per-call visited set ////////////////////////////////////////////////////////////////////////

        "repeating an estimate on the same object gives the same answer" {
            val subject = ObjectSizeEstimator()
            val value = listOf(EstProbe(1, 2), EstProbe(3, 4))

            val first = subject.estimate(value)

            // the visited set used to survive between calls, so the second answer was 0
            subject.estimate(value) shouldBe first
            subject.estimate(value) shouldBe first
        }

        "one estimate does not shrink the next" {
            val subject = ObjectSizeEstimator()

            subject.estimate(listOf(EstProbe(1, 2), EstProbe(3, 4)))

            // a structurally equal but freshly built graph must be measured from scratch
            val fresh = subject.estimate(listOf(EstProbe(1, 2), EstProbe(3, 4)))
            val reference = ObjectSizeEstimator().estimate(listOf(EstProbe(1, 2), EstProbe(3, 4)))

            fresh shouldBe reference
        }

        // Identity, not equality //////////////////////////////////////////////////////////////////////

        "two equal but distinct objects are both counted" {
            val subject = ObjectSizeEstimator()

            val twoDistinct = subject.estimate(listOf(EstProbe(1, 2), EstProbe(1, 2)))
            val twoDifferent = ObjectSizeEstimator().estimate(listOf(EstProbe(1, 2), EstProbe(3, 4)))

            // both lists hold two real objects, so both cost the same - structural matching used to
            // charge the second equal one zero bytes
            twoDistinct shouldBe twoDifferent
        }

        "the same object referenced twice is counted once" {
            val shared = EstProbe(1, 2)

            val once = ObjectSizeEstimator().estimate(listOf(shared, shared))
            val twice = ObjectSizeEstimator().estimate(listOf(EstProbe(1, 2), EstProbe(3, 4)))

            // one object plus a second pointer must be cheaper than two objects
            (once < twice) shouldBe true
        }

        "a cyclic graph still terminates" {
            val a = EstCycle("a")
            val b = EstCycle("b")
            a.other = b
            b.other = a

            ObjectSizeEstimator().estimate(a) shouldBeGreaterThan 0L
        }
    }
}

private data class EstProbe(val x: Int, val y: Int)

private class EstCycle(val name: String) {
    var other: EstCycle? = null
}
