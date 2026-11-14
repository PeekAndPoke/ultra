package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/** ULTRA-00341 batch 3b-2: the stored shape `{from, duration, to}`, and the fallback for the old `{from, to}`. */
@Suppress("unused")
class MpLocalTimeSlotSerializationSpec : StringSpec({

    fun t(hour: Int, minute: Int = 0) = MpLocalTime.of(hour, minute)
    fun encode(slot: MpLocalTimeSlot) = Json.encodeToString(MpLocalTimeSlot.serializer(), slot)
    fun decode(json: String) = Json.decodeFromString(MpLocalTimeSlot.serializer(), json)

    val night = MpLocalTimeSlot.of(t(22), 4.hours)

    "written as {from, duration, to} in milliseconds; to is derived" {
        encode(night) shouldBe """{"from":79200000,"duration":14400000,"to":7200000}"""
        encode(MpLocalTimeSlot.completeDay) shouldBe """{"from":0,"duration":86400000,"to":86400000}"""
        encode(MpLocalTimeSlot.of(t(10), Duration.ZERO)) shouldBe """{"from":36000000,"duration":0,"to":36000000}"""
    }

    "read back equal; when duration is present, a disagreeing to is ignored" {
        decode(encode(night)) shouldBe night
        decode(encode(MpLocalTimeSlot.completeDay)) shouldBe MpLocalTimeSlot.completeDay
        decode("""{"from":79200000,"duration":14400000,"to":0}""") shouldBe night
        decode("""{"from":79200000,"duration":-5}""") shouldBe MpLocalTimeSlot.of(t(22), Duration.ZERO)
        decode("""{"from":0,"duration":-1,"to":3600000}""") shouldBe MpLocalTimeSlot.of(t(0), Duration.ZERO)
    }

    "an infinite duration is stored as 2^53 - 1 ms, which a JS client reads exactly, and reads back infinite" {
        val open = MpLocalTimeSlot.of(t(10), Duration.INFINITE)

        encode(open) shouldBe """{"from":36000000,"duration":9007199254740991,"to":36000000}"""
        decode(encode(open)) shouldBe open
        decode("""{"from":36000000,"duration":9223372036854775807}""").isInfinite shouldBe true
    }

    "the old shape {from, to}: after from is the difference, before from is overnight, equal is empty" {
        decode("""{"from":79200000,"to":7200000}""") shouldBe night
        decode("""{"from":36000000,"to":43200000}""") shouldBe MpLocalTimeSlot.of(t(10), 2.hours)
        decode("""{"from":36000000,"to":36000000}""").isEmpty shouldBe true
        decode("""{"from":86400000,"to":86400000}""").isEmpty shouldBe true
        decode("""{"from":0,"to":86400000}""") shouldBe MpLocalTimeSlot.completeDay
        // the completeDay of versions before ULTRA-00341 ended at 23:59:59.999
        decode("""{"from":0,"to":86399999}""").duration shouldBe 86_399_999.milliseconds
    }

    "neither duration nor to is an error, as a missing field was before" {
        shouldThrow<SerializationException> { decode("""{"from":0}""") }
    }

    "only an ABSENT duration falls back to to; an explicit null (a JS client's Infinity or NaN) is an error" {
        shouldThrow<SerializationException> { decode("""{"from":0,"duration":null,"to":3600000}""") }
        shouldThrow<SerializationException> { decode("""{"from":0,"duration":null}""") }
    }

    "inside a list" {
        val list = listOf(night, MpLocalTimeSlot.completeDay)
        val serializer = ListSerializer(MpLocalTimeSlot.serializer())

        Json.decodeFromString(serializer, Json.encodeToString(serializer, list)) shouldBe list
    }
})
