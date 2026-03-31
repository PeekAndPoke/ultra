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
    }
}
