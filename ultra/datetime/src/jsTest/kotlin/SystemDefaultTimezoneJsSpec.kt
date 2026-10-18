package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.js.Date

@Suppress("unused")
class SystemDefaultTimezoneJsSpec : StringSpec({

    "systemDefault is the browser's IANA zone, not kotlinx's \"SYSTEM\" placeholder" {
        MpTimezone.systemDefault.id shouldNotBe "SYSTEM"
    }

    "systemDefault has the offset the browser's own Date reports" {
        listOf("2022-01-15T12:00:00Z", "2022-07-01T12:00:00Z").forEach { iso ->
            val instant = MpInstant.parse(iso)
            val date = Date(instant.toEpochMillis().toDouble())

            // getTimezoneOffset() is minutes BEHIND UTC, hence the sign flip
            MpTimezone.systemDefault.offsetSecondsAt(instant) shouldBe -date.getTimezoneOffset() * 60
        }
    }
})
