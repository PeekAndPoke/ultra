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

        "Byte estimates to BYTE_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate(0.toByte()) shouldBe ObjectSizeEstimatorImpl.BYTE_SIZE
        }

        "Char estimates to CHAR_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate('Z') shouldBe ObjectSizeEstimatorImpl.CHAR_SIZE
        }

        "Short estimates to SHORT_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate(99.toShort()) shouldBe ObjectSizeEstimatorImpl.SHORT_SIZE
        }

        "Int estimates to INT_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate(42) shouldBe ObjectSizeEstimatorImpl.INT_SIZE
        }

        "Long estimates to LONG_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate(42L) shouldBe ObjectSizeEstimatorImpl.LONG_SIZE
        }

        "Float estimates to FLOAT_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate(1.5f) shouldBe ObjectSizeEstimatorImpl.FLOAT_SIZE
        }

        "Double estimates to DOUBLE_SIZE" {
            val estimator = ObjectSizeEstimator()

            estimator.estimate(3.14) shouldBe ObjectSizeEstimatorImpl.DOUBLE_SIZE
        }

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

        "ShortArray estimate is arrayHeader + SHORT_SIZE * length" {
            val estimator = ObjectSizeEstimator()
            val arr = shortArrayOf(1, 2, 3, 4)

            estimator.estimate(arr) shouldBe cfg.arrayHeader + 4 * ObjectSizeEstimatorImpl.SHORT_SIZE
        }

        "CharArray estimate is arrayHeader + CHAR_SIZE * length" {
            val estimator = ObjectSizeEstimator()
            val arr = charArrayOf('a', 'b')

            estimator.estimate(arr) shouldBe cfg.arrayHeader + 2 * ObjectSizeEstimatorImpl.CHAR_SIZE
        }

        "IntArray estimate is arrayHeader + INT_SIZE * length" {
            val estimator = ObjectSizeEstimator()
            val arr = intArrayOf(1, 2, 3)

            estimator.estimate(arr) shouldBe cfg.arrayHeader + 3 * ObjectSizeEstimatorImpl.INT_SIZE
        }

        "LongArray estimate is arrayHeader + LONG_SIZE * length" {
            val estimator = ObjectSizeEstimator()
            val arr = longArrayOf(100L)

            estimator.estimate(arr) shouldBe cfg.arrayHeader + 1 * ObjectSizeEstimatorImpl.LONG_SIZE
        }

        "FloatArray estimate is arrayHeader + FLOAT_SIZE * length" {
            val estimator = ObjectSizeEstimator()
            val arr = floatArrayOf(1.0f, 2.0f)

            estimator.estimate(arr) shouldBe cfg.arrayHeader + 2 * ObjectSizeEstimatorImpl.FLOAT_SIZE
        }

        "DoubleArray estimate is arrayHeader + DOUBLE_SIZE * length" {
            val estimator = ObjectSizeEstimator()
            val arr = doubleArrayOf(1.0, 2.0, 3.0)

            estimator.estimate(arr) shouldBe cfg.arrayHeader + 3 * ObjectSizeEstimatorImpl.DOUBLE_SIZE
        }

        "empty Collection estimate is objectHeader only" {
            val estimator = ObjectSizeEstimator()
            val list = emptyList<Int>()

            estimator.estimate(list) shouldBe cfg.objectHeader
        }

        "Collection estimate includes element sizes" {
            val estimator = ObjectSizeEstimator()
            val list = listOf(1, 2, 3)

            val expected = cfg.objectHeader +
                    3 * cfg.pointerSize +
                    3 * ObjectSizeEstimatorImpl.INT_SIZE

            estimator.estimate(list) shouldBe expected
        }

        "Map estimate includes key and value sizes" {
            val estimator = ObjectSizeEstimator()
            val map = mapOf(1 to 2)

            val expected = cfg.objectHeader +
                    1 * 2 * cfg.pointerSize +
                    ObjectSizeEstimatorImpl.INT_SIZE + // key
                    ObjectSizeEstimatorImpl.INT_SIZE   // value

            estimator.estimate(map) shouldBe expected
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
