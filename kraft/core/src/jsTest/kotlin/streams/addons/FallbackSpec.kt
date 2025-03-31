package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FallbackSpec : StringSpec({

    "fallbackTo constant" {

        val source = StreamSource<Int?>(10)

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

        val source = StreamSource<Int?>(10)

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
