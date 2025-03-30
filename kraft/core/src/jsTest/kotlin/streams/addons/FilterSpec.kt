package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FilterSpec : StringSpec({

    "filter without initial value" {

        val source = StreamSource(10)

        val mapped = source.filter { it > 10 }

        val received = mutableListOf<Int?>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be 'null'") {
            received shouldBe listOf(null)
        }

        withClue("The next input must pass the filter") {
            source(11)

            received shouldBe listOf(null, 11)
        }

        withClue("The next input must not pass the filter") {
            source(10)

            received shouldBe listOf(null, 11)
        }

        unsubscribe()
    }

    "filter with initial value" {

        val source = StreamSource(10)

        val mapped = source.filter(0) { it > 10 }

        val received = mutableListOf<Int?>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be the initial value") {
            received shouldBe listOf(0)
        }

        withClue("The next input must pass the filter") {
            source(11)

            received shouldBe listOf(0, 11)
        }

        withClue("The next input must not pass the filter") {
            source(10)

            received shouldBe listOf(0, 11)
        }

        unsubscribe()
    }

    "filterNotNull with first value on stream being 'null'" {

        val source = StreamSource<Int?>(null)

        val mapped = source.filterNotNull(0)

        val received = mutableListOf<Int>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be '0'") {
            received shouldBe listOf(0)
        }

        withClue("The next input must pass the filter") {
            source(11)

            received shouldBe listOf(0, 11)
        }

        withClue("The next input must not pass the filter") {
            source(null)

            received shouldBe listOf(0, 11)
        }

        unsubscribe()
    }

    "filterIsInstance without initial value" {

        abstract class Data

        data class A(val x: Int) : Data()
        data class B(val x: Int) : Data()

        val source = StreamSource<Data?>(null)

        val mapped: Stream<B?> = source.filterIsInstance()

        val received = mutableListOf<B?>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be 'null'") {
            received shouldBe listOf(null)
        }

        withClue("The next input must pass the filter") {
            source(B(10))

            received shouldBe listOf(null, B(10))
        }

        withClue("The next input must not pass the filter") {
            source(A(10))

            received shouldBe listOf(null, B(10))
        }

        unsubscribe()
    }

    "filterIsInstance with initial value" {

        abstract class Data

        data class A(val x: Int) : Data()
        data class B(val x: Int) : Data()

        val source = StreamSource<Data?>(null)

        val mapped: Stream<B> = source.filterIsInstance(B(0))

        val received = mutableListOf<B?>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The first received value must be the initial value") {
            received shouldBe listOf(B(0))
        }

        withClue("The next input must pass the filter") {
            source(B(10))

            received shouldBe listOf(B(0), B(10))
        }

        withClue("The next input must not pass the filter") {
            source(A(10))

            received shouldBe listOf(B(0), B(10))
        }

        unsubscribe()
    }
})
