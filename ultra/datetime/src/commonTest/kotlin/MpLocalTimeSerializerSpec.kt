package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class MpLocalTimeSerializerSpec : StringSpec({

    val json = Json

    "Serialize" {

        val input = MpLocalTime.ofMilliSeconds(milliSecondsOfDay = 123)

        val result = json.encodeToString(
            MpLocalTime.serializer(),
            input
        )

        result shouldBe "123"
    }

    "De-Serialize" {

        val input = "123"

        val result = json.decodeFromString(
            MpLocalTime.serializer(),
            input
        )

        result shouldBe MpLocalTime.ofMilliSeconds(123)
    }

    "De-Serialize wraps values outside the day, also beyond the Int range" {
        json.decodeFromString(MpLocalTime.serializer(), "-1000") shouldBe MpLocalTime.of(23, 59, 59)
        json.decodeFromString(MpLocalTime.serializer(), "5000000000") shouldBe MpLocalTime.of(20, 53, 20)
    }
})
