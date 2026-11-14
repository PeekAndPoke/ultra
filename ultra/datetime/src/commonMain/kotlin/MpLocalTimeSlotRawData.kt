package io.peekandpoke.ultra.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration.Companion.milliseconds

/**
 * The stored shape of an [MpLocalTimeSlot], in milliseconds: `{from, duration, to}` (ULTRA-00341).
 *
 * [to] is derived and written for queries and readers; it is read only when [duration] is missing, i.e. from the shape
 * `{from, to}` written before ULTRA-00341 (`to` after `from` is the difference, before `from` overnight, equal empty).
 * An infinite duration is written as [MpLocalTimeSlot.INFINITE_DURATION_MILLIS]. Public for the same reason as
 * [MpDateTimeRawData]: slumber declares it through `@Slumber.As`, so karango and monko see it as their query paths.
 */
@Serializable
data class MpLocalTimeSlotRawData(
    val from: Long,
    val duration: Long? = null,
    val to: Long? = null,
)

/** kotlinx serializer for [MpLocalTimeSlot]: writes [MpLocalTimeSlotRawData], reads it and the old `{from, to}`. */
object MpLocalTimeSlotSerializer : KSerializer<MpLocalTimeSlot> {

    override val descriptor: SerialDescriptor = MpLocalTimeSlotRawData.serializer().descriptor

    override fun serialize(encoder: Encoder, value: MpLocalTimeSlot) {
        encoder.encodeSerializableValue(MpLocalTimeSlotRawData.serializer(), value.toRawData())
    }

    override fun deserialize(decoder: Decoder): MpLocalTimeSlot {
        val raw = decoder.decodeSerializableValue(MpLocalTimeSlotRawData.serializer())

        return raw.toSlotOrNull() ?: throw SerializationException("An MpLocalTimeSlot needs 'duration' or 'to'")
    }
}

/** The stored shape of this slot. */
fun MpLocalTimeSlot.toRawData(): MpLocalTimeSlotRawData = MpLocalTimeSlotRawData(
    from = from.inWholeMilliSeconds().toLong(),
    duration = if (isInfinite) MpLocalTimeSlot.INFINITE_DURATION_MILLIS else duration.inWholeMilliseconds,
    to = to.inWholeMilliSeconds().toLong(),
)

/** The slot from the stored shape: [MpLocalTimeSlotRawData.duration] when present, else the old `{from, to}`. */
fun MpLocalTimeSlotRawData.toSlotOrNull(): MpLocalTimeSlot? {
    val start = storedTime(from)

    return when {
        duration != null -> MpLocalTimeSlot.of(from = start, duration = duration.milliseconds)
        to != null -> MpLocalTimeSlot.of(from = start, to = storedTime(to))
        else -> null
    }
}

/** A stored millisecond of the day: exactly one day is 24:00, every other value wraps (as [MpLocalTimeSerializer]). */
private fun storedTime(millis: Long): MpLocalTime = when (millis) {
    MpLocalTime.MILLIS_PER_DAY.toLong() -> MpLocalTime.EndOfDay
    else -> MpLocalTime.ofMilliSeconds(millis.mod(MpLocalTime.MILLIS_PER_DAY.toLong()).toInt())
}
