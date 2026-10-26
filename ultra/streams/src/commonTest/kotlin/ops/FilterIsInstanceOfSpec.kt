package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource

private interface Tagged

private abstract class Data

private data class A(val x: Int) : Data(), Tagged

private data class B(val x: Int) : Data()

/** `filterIsInstanceOf(KClass)` (ULTRA-00241): the type comes from the class, no type arguments. */
class FilterIsInstanceOfSpec : StringSpec({

    "filterIsInstanceOf publishes only instances of the class, null until the first" {
        val source = StreamSource<Data?>(null)
        val filtered: Stream<B?> = source.filterIsInstanceOf(B::class)
        val received = mutableListOf<B?>()

        val unsubscribe = filtered.subscribeToStream { received.add(it) }

        source(B(1))
        source(A(2))
        source(null)
        source(B(3))

        received shouldBe listOf(null, B(1), B(3))
        filtered() shouldBe B(3)

        unsubscribe()
    }

    "filterIsInstanceOf with an initial value publishes it until the first instance" {
        val source = StreamSource<Data?>(A(0))
        val filtered: Stream<B> = source.filterIsInstanceOf(B::class, initial = B(0))
        val received = mutableListOf<B>()

        val unsubscribe = filtered.subscribeToStream { received.add(it) }

        source(A(1))
        source(B(2))
        source(A(3))

        received shouldBe listOf(B(0), B(2))

        unsubscribe()
    }

    "filterIsInstanceOf lets subclasses and implementations through: a supertype and an interface" {
        val source = StreamSource<Any?>(A(1))
        val data: Stream<Data?> = source.filterIsInstanceOf(Data::class)
        val tagged: Stream<Tagged?> = source.filterIsInstanceOf(Tagged::class)
        val receivedData = mutableListOf<Data?>()
        val receivedTagged = mutableListOf<Tagged?>()

        val unsubscribeData = data.subscribeToStream { receivedData.add(it) }
        val unsubscribeTagged = tagged.subscribeToStream { receivedTagged.add(it) }

        source(B(2))
        source("x")

        receivedData shouldBe listOf(A(1), B(2))
        receivedTagged shouldBe listOf(A(1))

        unsubscribeData()
        unsubscribeTagged()
    }

    "filterIsInstanceOf works on a Stream<Any?>, where a KClass would also fit filterIsInstance(initial)" {
        val source = StreamSource<Any?>("text")
        val strings: Stream<String?> = source.filterIsInstanceOf(String::class)
        val received = mutableListOf<String?>()

        val unsubscribe = strings.subscribeToStream { received.add(it) }

        source(42)
        source("new text")

        received shouldBe listOf("text", "new text")

        unsubscribe()
    }
})
