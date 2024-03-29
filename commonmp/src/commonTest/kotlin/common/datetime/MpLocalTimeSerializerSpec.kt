package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

@Suppress("unused")
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
})
