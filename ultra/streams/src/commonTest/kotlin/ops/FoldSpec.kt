package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.StreamSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FoldSpec : StringSpec({

    "fold accumulates values starting from initial" {

        val source = StreamSource(1)
        val folded = source.fold(0) { acc, next -> acc + next }

        val received = mutableListOf<Int>()

        val unsub = folded.subscribeToStream {
            received.add(it)
        }

        source(2)
        source(3)

        // Initial subscribe triggers fold(0, 1) = 1, then fold(1, 2) = 3, fold(3, 3) = 6
        received shouldBe listOf(1, 3, 6)

        unsub()
    }

    "fold returns the accumulator initial before subscription" {

        val source = StreamSource(10)
        val folded = source.fold(0) { acc, next -> acc + next }

        // Before subscribing, fold returns its initial accumulator value
        folded() shouldBe 0
    }

    "foldNotNull skips null values" {

        val source = StreamSource<Int?>(1)
        val folded = source.foldNotNull(0) { acc, next -> acc + next }

        val received = mutableListOf<Int>()

        val unsub = folded.subscribeToStream {
            received.add(it)
        }

        source(null)
        source(2)
        source(null)
        source(3)

        // null values are skipped: fold(0,1)=1, skip, fold(1,2)=3, skip, fold(3,3)=6
        received shouldBe listOf(1, 3, 6)

        unsub()
    }

    "fold state persists across multiple publishes" {

        val source = StreamSource("a")
        val folded = source.fold("") { acc, next -> acc + next }

        val unsub = folded.subscribeToStream { }

        source("b")
        source("c")

        folded() shouldBe "abc"

        unsub()
    }
})
