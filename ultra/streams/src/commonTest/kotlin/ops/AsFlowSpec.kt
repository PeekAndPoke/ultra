package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.flow.first

class AsFlowSpec : StringSpec({

    "asFlow emits the current value and cleans up after cancellation" {

        val source = StreamSource(10)

        val received = source.asFlow().first()

        received shouldBe 10

        source.subscriptions.shouldBeEmpty()
    }
})
