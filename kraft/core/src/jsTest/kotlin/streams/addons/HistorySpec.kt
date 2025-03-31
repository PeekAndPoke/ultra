package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.StreamSource
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HistorySpec : StringSpec({

    "history()" {

        val source = StreamSource(10.0)

        val mapped = source.history(3)

        withClue("Initially the history must be empty") {
            mapped() shouldBe emptyList()
            mapped() shouldBe emptyList()
        }

        val received = mutableListOf<List<Double>>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("After subscribing the history must 1 element") {
            mapped() shouldBe listOf(10.0)
            mapped() shouldBe listOf(10.0)

            received shouldBe listOf(
                listOf(10.0),
            )
        }

        withClue("Sending values must fill and overflow the history") {
            source(20.0)
            mapped() shouldBe listOf(10.0, 20.0)

            source(30.0)
            mapped() shouldBe listOf(10.0, 20.0, 30.0)

            source(40.0)
            mapped() shouldBe listOf(20.0, 30.0, 40.0)

            received shouldBe listOf(
                listOf(10.0),
                listOf(10.0, 20.0),
                listOf(10.0, 20.0, 30.0),
                listOf(20.0, 30.0, 40.0),
            )
        }

        unsubscribe()
    }

    "historyOfNonNull()" {

        val source = StreamSource<Double?>(10.0)

        val mapped = source.historyOfNonNull(3)

        withClue("Initially the history must be empty") {
            mapped() shouldBe emptyList()
            mapped() shouldBe emptyList()
        }

        val received = mutableListOf<List<Double>>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("After subscribing the history must 1 element") {
            mapped() shouldBe listOf(10.0)
            mapped() shouldBe listOf(10.0)

            received shouldBe listOf(
                listOf(10.0),
            )
        }

        withClue("Sending values must fill and overflow the history") {
            source(null)
            mapped() shouldBe listOf(10.0)

            source(20.0)
            mapped() shouldBe listOf(10.0, 20.0)

            source(null)
            mapped() shouldBe listOf(10.0, 20.0)

            source(30.0)
            mapped() shouldBe listOf(10.0, 20.0, 30.0)

            source(null)
            mapped() shouldBe listOf(10.0, 20.0, 30.0)

            source(40.0)
            mapped() shouldBe listOf(20.0, 30.0, 40.0)

            source(null)
            mapped() shouldBe listOf(20.0, 30.0, 40.0)

            received shouldBe listOf(
                listOf(10.0),
                listOf(10.0, 20.0),
                listOf(10.0, 20.0, 30.0),
                listOf(20.0, 30.0, 40.0),
            )
        }

        unsubscribe()
    }
})
