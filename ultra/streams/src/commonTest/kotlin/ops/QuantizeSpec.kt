package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource

/** The `quantize` stream operators (ULTRA-00242). */
class QuantizeSpec : StringSpec({

    "every value is snapped to the grid; the offset shifts the grid" {
        val source = StreamSource(0.26)
        val snapped = source.quantize(size = 0.1)
        val shifted = source.quantize(size = 0.1, offset = 0.05)

        snapped() shouldBe (0.3 plusOrMinus 1e-12)
        shifted() shouldBe (0.25 plusOrMinus 1e-12)

        source(0.74)

        snapped() shouldBe (0.7 plusOrMinus 1e-12)
        shifted() shouldBe (0.75 plusOrMinus 1e-12)
    }

    "on a nullable stream, null passes as it is" {
        val source = StreamSource<Double?>(null)
        val snapped = source.quantize(size = 0.5)
        val seen = mutableListOf<Double?>()

        snapped.subscribeToStream { seen.add(it) }

        source(1.3)
        source(null)

        seen shouldBe listOf(null, 1.5, null)
    }
})
