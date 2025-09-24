package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FallbackSpec : StringSpec({

    "fallbackTo constant" {

        val source = StreamSource.Companion<Int?>(10)

        val mapped = source.fallbackTo(0)

        val received = mutableListOf<Int>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)

        source(null)
        source(20)

        received shouldBe listOf(10, 0, 20)

        unsubscribe()
    }

    "fallbackBy callback" {

        val source = StreamSource.Companion<Int?>(10)

        var next = 0
        val mapped: Stream<Int> = source.fallbackBy { ++next }

        val received = mutableListOf<Int>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)

        source(null)
        source(20)
        source(null)

        received shouldBe listOf(10, 1, 20, 2)

        unsubscribe()
    }
})
