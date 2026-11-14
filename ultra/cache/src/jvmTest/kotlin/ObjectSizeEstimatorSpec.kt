package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

private class EstimatorOuter(val payload: LongArray) {
    inner class Inner(val x: Int)
}

private class EstimatorPoint(val x: Int, val y: Int)

private interface EstimatorContent

private class EstimatorPayload(val data: LongArray) : EstimatorContent

private class EstimatorDoc(content: EstimatorContent) : EstimatorContent by content

private class EstimatorText(val s: String)

class ObjectSizeEstimatorSpec : StringSpec({
    timeout = TEST_TIMEOUT_MS

    val estimator = ObjectSizeEstimator()

    "null estimates to NULL_SIZE" {
        estimator.estimate(null) shouldBe ObjectSizeEstimatorImpl.NULL_SIZE
    }

    "primitives estimate to their sizes" {
        estimator.estimate(true) shouldBe ObjectSizeEstimatorImpl.BOOL_SIZE
        estimator.estimate(42.toByte()) shouldBe ObjectSizeEstimatorImpl.BYTE_SIZE
        estimator.estimate('A') shouldBe ObjectSizeEstimatorImpl.CHAR_SIZE
        estimator.estimate(42.toShort()) shouldBe ObjectSizeEstimatorImpl.SHORT_SIZE
        estimator.estimate(42) shouldBe ObjectSizeEstimatorImpl.INT_SIZE
        estimator.estimate(42L) shouldBe ObjectSizeEstimatorImpl.LONG_SIZE
        estimator.estimate(3.14f) shouldBe ObjectSizeEstimatorImpl.FLOAT_SIZE
        estimator.estimate(3.14) shouldBe ObjectSizeEstimatorImpl.DOUBLE_SIZE
    }

    "string estimate includes header + char array" {
        val cfg = ObjectSizeEstimatorImpl.EstimatorConfig()
        val str = "hello"
        val expected =
            cfg.objectHeader + cfg.pointerSize + cfg.arrayHeader + str.length * ObjectSizeEstimatorImpl.CHAR_SIZE

        estimator.estimate(str) shouldBe expected
    }

    "empty string is smaller than non-empty string" {
        val emptySize = estimator.estimate("")
        val nonEmptySize = estimator.estimate("hello world")

        nonEmptySize shouldBeGreaterThan emptySize
    }

    "primitive arrays estimate correctly" {
        val cfg = ObjectSizeEstimatorImpl.EstimatorConfig()

        estimator.estimate(intArrayOf(1, 2, 3)) shouldBe cfg.arrayHeader + 3 * ObjectSizeEstimatorImpl.INT_SIZE
        estimator.estimate(booleanArrayOf(true, false)) shouldBe cfg.arrayHeader + 2 * ObjectSizeEstimatorImpl.BOOL_SIZE
        estimator.estimate(doubleArrayOf(1.0)) shouldBe cfg.arrayHeader + 1 * ObjectSizeEstimatorImpl.DOUBLE_SIZE
    }

    "collection estimate includes elements" {
        val list = listOf(1, 2, 3)
        val size = estimator.estimate(list)

        // Should be > object header + pointers (the int elements add their sizes)
        val cfg = ObjectSizeEstimatorImpl.EstimatorConfig()
        size shouldBeGreaterThan cfg.objectHeader
    }

    "map estimate includes keys and values" {
        val map = mapOf("a" to 1, "b" to 2)
        val size = estimator.estimate(map)

        val cfg = ObjectSizeEstimatorImpl.EstimatorConfig()
        size shouldBeGreaterThan cfg.objectHeader
    }

    "data class estimate uses reflection" {
        data class Point(val x: Int, val y: Int)

        val size = estimator.estimate(Point(1, 2))

        // Should be > 0 — object header + fields
        size shouldBeGreaterThan 0L
    }

    "same object counted only once" {
        val shared = "shared-string"
        val list = listOf(shared, shared, shared)

        // The string should only be counted once (seen check), rest are pointer cost only
        val singleSize = ObjectSizeEstimator().estimate(shared)
        val listSize = ObjectSizeEstimator().estimate(list)

        // List overhead + 3 pointers + 1x string (others are seen)
        val cfg = ObjectSizeEstimatorImpl.EstimatorConfig()
        val expectedApprox = cfg.objectHeader + 3 * cfg.pointerSize + singleSize
        listSize shouldBe expectedApprox
    }

    "an inner class is not charged its outer instance" {
        val cfg = ObjectSizeEstimatorImpl.EstimatorConfig()
        val inner = EstimatorOuter(LongArray(10_000)).Inner(1)

        // the synthetic this$0 is skipped, so only `x` is charged - not the outer's 80 KB payload
        ObjectSizeEstimator().estimate(inner) shouldBe cfg.objectHeader + cfg.pointerSize + 4L
    }

    "an anonymous object is charged its captured locals" {
        val captured = LongArray(10_000)
        val anonymous = object {
            val y = 1

            @Suppress("unused")
            fun size() = captured.size
        }

        // header, two references (y and the synthetic $captured), y, then the array
        ObjectSizeEstimator().estimate(anonymous) shouldBe 16L + 2 * 8L + 4L + (24L + 10_000 * 8L)
    }

    "a class delegating by `by` is charged its delegate" {
        val doc = EstimatorDoc(EstimatorPayload(LongArray(1_000)))

        // the doc: header and one reference (the synthetic $$delegate_0); the payload: header,
        // one reference, the array
        ObjectSizeEstimator().estimate(doc) shouldBe (16L + 8L) + (16L + 8L + 24L + 1_000 * 8L)
    }

    "the field lookup is resolved per class" {
        // Alternating two classes catches a memo that hands one class's fields to another
        repeat(2) {
            ObjectSizeEstimator().estimate(EstimatorPoint(1, 2)) shouldBe 16L + 2 * 8L + 4L + 4L
            ObjectSizeEstimator().estimate(EstimatorText("ab")) shouldBe 16L + 8L + (16L + 8L + 24L + 2 * 2L)
        }
    }

    "larger objects estimate larger" {
        data class Small(val x: Int)
        data class Large(val a: Int, val b: Int, val c: String, val d: List<Int>)

        val smallSize = ObjectSizeEstimator().estimate(Small(1))
        val largeSize = ObjectSizeEstimator().estimate(Large(1, 2, "hello", listOf(1, 2, 3)))

        largeSize shouldBeGreaterThan smallSize
    }
})
