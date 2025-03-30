package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.StreamSource
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DistinctSpec : StringSpec({

    "distinct(strict = false)" {

        data class X(val v: Int)

        val source = StreamSource(X(10))

        val mapped = source.distinct(strict = false)

        val received = mutableListOf<X>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be '10'") {
            received shouldBe listOf(X(10))
        }

        withClue("The next input must pass the filter") {
            source(X(11))

            received shouldBe listOf(X(10), X(11))
        }

        withClue("The next input must not pass the filter") {
            source(X(11))

            received shouldBe listOf(X(10), X(11))
        }

        withClue("The next input must pass the filter") {
            source(X(10))

            received shouldBe listOf(X(10), X(11), X(10))
        }

        unsubscribe()
    }

    "distinct()" {

        data class X(val v: Int)

        val source = StreamSource(X(10))

        val mapped = source.distinct()

        val received = mutableListOf<X>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be '10'") {
            received shouldBe listOf(X(10))
        }

        withClue("The next input must pass the filter") {
            source(X(11))

            received shouldBe listOf(X(10), X(11))
        }

        withClue("The next input must not pass the filter") {
            source(X(11))

            received shouldBe listOf(X(10), X(11))
        }

        withClue("The next input must pass the filter") {
            source(X(10))

            received shouldBe listOf(X(10), X(11), X(10))
        }

        unsubscribe()
    }

    "distinct(strict = true)" {

        data class X(val v: Int)

        val source = StreamSource(X(10))

        val mapped = source.distinct(strict = true)

        val received = mutableListOf<X>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be '10'") {
            received shouldBe listOf(X(10))
        }

        val reused = X(11)

        withClue("The next input must pass the filter") {
            source(reused)

            received shouldBe listOf(X(10), X(11))
        }

        withClue("The next input must not pass the filter") {
            source(reused)

            received shouldBe listOf(X(10), X(11))
        }

        withClue("The next input must pass the filter") {
            source(X(11))

            received shouldBe listOf(X(10), X(11), X(11))
        }

        unsubscribe()
    }

    "distinctStrict()" {

        data class X(val v: Int)

        val source = StreamSource(X(10))

        val mapped = source.distinctStrict()

        val received = mutableListOf<X>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be '10'") {
            received shouldBe listOf(X(10))
        }

        val reused = X(11)

        withClue("The next input must pass the filter") {
            source(reused)

            received shouldBe listOf(X(10), X(11))
        }

        withClue("The next input must not pass the filter") {
            source(reused)

            received shouldBe listOf(X(10), X(11))
        }

        withClue("The next input must pass the filter") {
            source(X(11))

            received shouldBe listOf(X(10), X(11), X(11))
        }

        unsubscribe()
    }
})
