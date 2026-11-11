package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource

/** `history` with the capacity as a stream (ULTRA-00271). */
class HistoryCapacitySpec : StringSpec({

    "a smaller capacity drops the oldest at once, a larger one keeps what there is" {
        val source = StreamSource(1)
        val capacity = StreamSource(3)
        val history = source.history(capacity)
        val seen = mutableListOf<List<Int>>()

        history.subscribeToStream { seen.add(it) }

        source(2)
        source(3)
        capacity(2)
        capacity(4)
        source(4)
        source(5)

        seen shouldBe listOf(
            listOf(1),
            listOf(1, 2),
            listOf(1, 2, 3),
            listOf(2, 3),
            listOf(2, 3, 4),
            listOf(2, 3, 4, 5),
        )
    }

    "a capacity of 0 or less records nothing and does not throw" {
        val source = StreamSource(1)
        val history = source.history(-1)
        val seen = mutableListOf<List<Int>>()

        history.subscribeToStream { seen.add(it) }
        source(2)

        seen shouldBe listOf(emptyList(), emptyList())
    }

    "historyOfNonNull skips null" {
        val source = StreamSource<Int?>(null)
        val history = source.historyOfNonNull(StreamSource(2))
        val seen = mutableListOf<List<Int>>()

        history.subscribeToStream { seen.add(it) }
        source(1)
        source(null)
        source(2)
        source(3)

        seen.last() shouldBe listOf(2, 3)
    }

    "the capacity is listened to only while someone listens; a read meanwhile is cut to it" {
        val source = StreamSource(1)
        val capacity = StreamSource(3)
        val history = source.history(capacity)

        val unsubscribe = history.subscribeToStream { }
        source(2)
        source(3)
        capacity.subscriptions.size shouldBe 1

        unsubscribe()
        capacity.subscriptions.size shouldBe 0

        capacity(1)
        history() shouldBe listOf(3)
    }
})
