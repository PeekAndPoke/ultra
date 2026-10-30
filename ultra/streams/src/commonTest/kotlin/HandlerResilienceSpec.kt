package io.peekandpoke.ultra.streams

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.ops.combinedWith
import io.peekandpoke.ultra.streams.ops.cutoffWhen
import io.peekandpoke.ultra.streams.ops.map
import io.peekandpoke.ultra.streams.ops.switchMap

class HandlerResilienceSpec : StringSpec({

    "A handler that throws on its first value is not kept: neither by a source nor by a wrapper, nor upstream" {
        val source = StreamSource(10)
        var called = 0

        runCatching { source.subscribeToStream { called++; error("I fail at once") } }.isFailure shouldBe true
        source(20)
        called shouldBe 1
        source.subscriptions.size shouldBe 0

        val mapped = source.map { it * 2 }
        var mappedCalls = 0
        runCatching { mapped.subscribeToStream { mappedCalls++; error("I fail at once") } }.isFailure shouldBe true
        source(30)
        mappedCalls shouldBe 1
        // The wrapper let go of its source too: nobody listens to it any more
        source.subscriptions.size shouldBe 0
    }

    "A handler that throws on its first value is not kept by combinedWith, cutoffWhen or switchMap either" {
        val operators: List<Pair<String, (Stream<Int>) -> Stream<Int>>> = listOf(
            "combinedWith" to { s -> s.combinedWith(StreamSource(0)) { a, b -> a + b } },
            "cutoffWhen" to { s -> s.cutoffWhen(StreamSource(false)) },
            "switchMap" to { s -> s.switchMap { StreamSource(it) } },
        )

        operators.forEach { (name, op) ->
            val source = StreamSource(1)
            var calls = 0

            runCatching { op(source).subscribeToStream { calls++; error("I fail at once") } }.isFailure shouldBe true
            source(2)

            withClue(name) {
                calls shouldBe 1
                source.subscriptions.size shouldBe 0
            }
        }
    }

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
