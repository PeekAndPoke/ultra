package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.StreamSource
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class IndexedSpec : StringSpec({

    "indexed() - NOT querying the value before subscribing" {

        val source = StreamSource(10.0)

        val mapped = source.indexed()

        val received = mutableListOf<Pair<Int, Double>>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received index must be 0") {
            mapped() shouldBe Pair(0, 10.0)
            mapped() shouldBe Pair(0, 10.0)

            received shouldBe listOf(Pair(0, 10.0))
        }

        withClue("The next received index must be 1") {
            source(20.0)

            mapped() shouldBe Pair(1, 20.0)
            mapped() shouldBe Pair(1, 20.0)

            received shouldBe listOf(Pair(0, 10.0), Pair(1, 20.0))
        }

        withClue("The next received index must be 2") {
            source(20.0)

            mapped() shouldBe Pair(2, 20.0)
            mapped() shouldBe Pair(2, 20.0)

            received shouldBe listOf(Pair(0, 10.0), Pair(1, 20.0), Pair(2, 20.0))
        }

        unsubscribe()
    }
})
