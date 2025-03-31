package de.peekandpoke.kraft.streams

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.firstOrNull

class StreamAsFlowSpec : StringSpec({

    "After the flow was closed the stream source must no have any subscriptions" {

        val source = StreamSource(10)

        val received = source.asFlow().firstOrNull()

        received shouldBe 10

        source.subscriptions.shouldBeEmpty()
    }

    "The collected values must be correct" {

        val source = StreamSource(1)

        val received = mutableListOf<Int>()
        val flow = source.asFlow()

        // Flow will be closed immediately
        flow.collect {
            received.add(it)
        }

        // Publishing a new value will not be collected
        source(2)

        received shouldBe listOf(1)
        source.subscriptions.shouldBeEmpty()
    }
})
