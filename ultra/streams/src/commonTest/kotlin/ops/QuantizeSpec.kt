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

    "a new size or offset re-snaps the current value at once (ULTRA-00271)" {
        val source = StreamSource(0.26)
        val size = StreamSource(0.1)
        val offset = StreamSource(0.0)
        val snapped = source.quantize(size = size, offset = offset)
        val seen = mutableListOf<Double>()

        snapped.subscribeToStream { seen.add(it) }

        size(0.5)
        offset(0.05)

        seen.size shouldBe 3
        seen[0] shouldBe (0.3 plusOrMinus 1e-12)
        seen[1] shouldBe (0.5 plusOrMinus 1e-12)
        seen[2] shouldBe (0.05 plusOrMinus 1e-12)
    }

    "the grid streams are listened to only while someone listens" {
        val source = StreamSource<Double?>(1.3)
        val size = StreamSource(0.5)
        val offset = StreamSource(0.0)
        val snapped = source.quantize(size = size, offset = offset)

        size.subscriptions.size shouldBe 0
        snapped() shouldBe (1.5 plusOrMinus 1e-12)

        val unsubscribe = snapped.subscribeToStream { }
        size.subscriptions.size shouldBe 1
        offset.subscriptions.size shouldBe 1

        unsubscribe()
        size.subscriptions.size shouldBe 0
        offset.subscriptions.size shouldBe 0
    }
})
