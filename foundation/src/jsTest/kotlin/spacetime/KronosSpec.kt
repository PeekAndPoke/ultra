package de.peekandpoke.ultra.foundation.spacetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.js.Date

@Suppress("unused")
class KronosSpec : StringSpec() {

    private fun Long.plusMinus(x: Long) = LongRange(this - x, this + x)

    init {
        "Kronos.system | millisNow()" {

            val subject = Kronos.system

            subject.millisNow() shouldBeInRange Date.now().toLong().plusMinus(100)
        }

        "Kronos.system.advancedBy | millisNow()" {

            val durationMs = 10_000L
            val subject = Kronos.system.advanceBy(durationMs)

            subject.millisNow() shouldBeInRange (Date.now().toLong() + durationMs).plusMinus(100)
        }

        "Kronos.system.advancedBy.advanceBy | millisNow()" {

            val durationMs1 = 10_000L
            val durationMs2 = 100_000L
            val subject = Kronos.system.advanceBy(durationMs1).advanceBy(durationMs2)

            subject.millisNow() shouldBeInRange (Date.now().toLong() + durationMs1 + durationMs2).plusMinus(100)
        }

        "Kronos.describe for Kronos.system" {

            val subject = Kronos.system.describe()

            subject shouldBe KronosDescriptor.SystemClock
        }

        "Kronos.describe for Kronos.system.advancedBy" {

            val duration = 10_000L

            val subject = Kronos.system.advanceBy(duration).describe()

            subject shouldBe KronosDescriptor.AdvancedBy(
                durationMs = duration,
                inner = KronosDescriptor.SystemClock
            )
        }

        "Kronos.systemUtc.advancedBy.advancedBy" {

            val duration1 = 10_000L
            val duration2 = 100_000L

            val subject = Kronos.system.advanceBy(duration1).advanceBy(duration2).describe()

            subject shouldBe KronosDescriptor.AdvancedBy(
                durationMs = duration2,
                inner = KronosDescriptor.AdvancedBy(
                    durationMs = duration1,
                    inner = KronosDescriptor.SystemClock,
                )
            )
        }
    }
}
