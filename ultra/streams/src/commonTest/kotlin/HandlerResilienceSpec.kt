package io.peekandpoke.ultra.streams

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.ops.map

class HandlerResilienceSpec : StringSpec({

    "A failing handler on StreamSource must not prevent other handlers from being notified" {

        val source = StreamSource(10)
        val received = mutableListOf<Int>()

        // This handler throws, but only on values after the initial subscribe
        var throwOnNext = false
        source.subscribeToStream {
            if (throwOnNext) throw RuntimeException("I fail")
        }

        source.subscribeToStream {
            received.add(it)
        }

        throwOnNext = true
        source(20)
        source(30)

        received shouldBe listOf(10, 20, 30)
    }

    "A failing handler on a mapped stream must not prevent other handlers from being notified" {

        val source = StreamSource(10)
        val mapped = source.map { it * 2 }
        val received = mutableListOf<Int>()

        var throwOnNext = false
        mapped.subscribeToStream {
            if (throwOnNext) throw RuntimeException("I fail")
        }

        mapped.subscribeToStream {
            received.add(it)
        }

        throwOnNext = true
        source(20)
        source(30)

        received shouldBe listOf(20, 40, 60)
    }

    "Multiple failing handlers still allow healthy handlers to receive values" {

        val source = StreamSource(1)
        val received = mutableListOf<Int>()

        var throwOnNext = false
        source.subscribeToStream { if (throwOnNext) throw RuntimeException("fail 1") }
        source.subscribeToStream { received.add(it) }
        source.subscribeToStream { if (throwOnNext) throw RuntimeException("fail 2") }
        source.subscribeToStream { received.add(it * 10) }

        throwOnNext = true
        source(2)

        received shouldBe listOf(1, 10, 2, 20)
    }
})
