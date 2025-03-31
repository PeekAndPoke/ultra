package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.StreamSource
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class MapAsyncSpec : StringSpec({

    "Mapping object with a function" {

        data class In(val v: Int)
        data class Out(val v: Int)

        val source = StreamSource(In(10))

        val mapped = source.mapAsync {
            delay(10)
            Out(v = it.v * 10)
        }

        val received = mutableListOf<Out?>()

        val unsubscribe = mapped.subscribeToStream {
            received.add(it)
        }

        withClue("The initial value must be 'null'") {
            received shouldBe listOf(null)
        }

        delay(1000)

        received shouldBe listOf(null, Out(100))

        source(In(20))
        source(In(20))

        delay(100)

        received shouldBe listOf(null, Out(100), Out(200), Out(200))

        unsubscribe()
    }
})
