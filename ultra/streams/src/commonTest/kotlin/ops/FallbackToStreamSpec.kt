package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource

/** `fallbackTo` with the fallback as a stream (ULTRA-00271). */
class FallbackToStreamSpec : StringSpec({

    "a new fallback is published at once while the stream is null" {
        val source = StreamSource<Int?>(null)
        val fallback = StreamSource(0)
        val stream = source.fallbackTo(fallback)
        val seen = mutableListOf<Int>()

        stream.subscribeToStream { seen.add(it) }

        fallback(1)
        source(5)
        source(null)

        seen shouldBe listOf(0, 1, 5, 1)
        stream() shouldBe 1
    }

    "the fallback is listened to only while someone listens" {
        val source = StreamSource<Int?>(null)
        val fallback = StreamSource(0)
        val stream = source.fallbackTo(fallback)

        fallback(7)
        fallback.subscriptions.size shouldBe 0
        stream() shouldBe 7

        val unsubscribe = stream.subscribeToStream { }
        fallback.subscriptions.size shouldBe 1

        unsubscribe()
        fallback.subscriptions.size shouldBe 0
    }
})
