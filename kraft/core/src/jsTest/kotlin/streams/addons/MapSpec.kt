package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.StreamSource
import de.peekandpoke.kraft.utils.identity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MapSpec : StringSpec({

    "Mapping with identity" {

        val source = StreamSource(10)

        val mapped = source.map(::identity)

        val received = mutableListOf<Int>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)

        source(20)
        source(20)

        received shouldBe listOf(10, 20, 20)

        unsubscribe()
    }

    "Mapping object with a function" {

        data class In(val v: Int)
        data class Out(val v: Int)

        val source = StreamSource(In(10))

        val mapped = source.map { Out(v = it.v * 10) }

        val received = mutableListOf<Out>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(Out(100))

        source(In(20))
        source(In(20))

        received shouldBe listOf(Out(100), Out(200), Out(200))

        unsubscribe()
    }
})
