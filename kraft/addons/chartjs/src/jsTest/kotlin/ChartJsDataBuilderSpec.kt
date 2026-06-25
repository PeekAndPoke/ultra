package io.peekandpoke.kraft.addons.chartjs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** Pure tests for the [ChartJsDataBuilder] DSL helpers — no Chart.js library load required. */
class ChartJsDataBuilderSpec : StringSpec({

    "numberLabels(count) produces 1..count as strings" {
        ChartJsDataBuilder().numberLabels(5).toList() shouldBe listOf("1", "2", "3", "4", "5")
    }

    "numberLabels(range) produces the range as strings" {
        ChartJsDataBuilder().numberLabels(2..4).toList() shouldBe listOf("2", "3", "4")
    }

    "nextColor cycles through the palette and wraps around" {
        val b = ChartJsDataBuilder()
        val firstPass = b.colors.indices.map { b.nextColor().value }
        firstPass shouldBe b.colors.toList()
        // after a full pass it wraps back to the first color
        b.nextColor().value shouldBe b.colors[0]
    }
})
