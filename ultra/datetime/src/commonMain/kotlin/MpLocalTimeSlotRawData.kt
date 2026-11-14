package io.peekandpoke.ultra.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration.Companion.milliseconds

/** Marks a [MpLocalTimeSlotRawData.duration] that was not in the input; no slot ever writes this value. */
private const val DURATION_ABSENT: Long = Long.MIN_VALUE

/**
 * The stored shape of an [MpLocalTimeSlot], in milliseconds: `{from, duration, to}` (ULTRA-00341).
 *
 * - [duration] decides; an infinite one is written as [MpLocalTimeSlot.INFINITE_DURATION_MILLIS].
 * - [to] is derived and written for queries and readers. It is read only when [duration] is ABSENT, i.e. from the
 *   shape `{from, to}` written before ULTRA-00341 (`to` after `from` is the difference, before `from` overnight, equal
 *   empty). An explicit `"duration": null` (what a TypeScript client's `Infinity` or `NaN` becomes) is an error.
 * - What a query on the stored [to] sees: an overnight slot has `to < from`; a slot ending at midnight has 86400000;
 *   a slot longer than a day has only the clock time of its end; an infinite or empty slot has `to = from`. Query
 *   [duration] for the length.
 * - Public for the same reason as [MpDateTimeRawData]: slumber declares it through `@Slumber.As`, so karango and
 *   monko see it as their query paths; the slumber codec decodes through [toSlotOrNull].
 */
@Serializable
data class MpLocalTimeSlotRawData(
    val from: Long,
    val duration: Long? = DURATION_ABSENT,
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

        return raw.toSlotOrNull()
            ?: throw SerializationException("An MpLocalTimeSlot needs a numeric 'duration', or 'to' without 'duration'")
    }
}

/** The stored shape of this slot. */
fun MpLocalTimeSlot.toRawData(): MpLocalTimeSlotRawData = MpLocalTimeSlotRawData(
    from = from.inWholeMilliSeconds().toLong(),
    duration = if (isInfinite) MpLocalTimeSlot.INFINITE_DURATION_MILLIS else duration.inWholeMilliseconds,
    to = to.inWholeMilliSeconds().toLong(),
)

/**
 * The slot from the stored shape, the one home of the decode rule for both codecs: [MpLocalTimeSlotRawData.duration]
 * when present; when it is absent, the old `{from, to}`; null for an explicit null duration or neither.
 */
fun MpLocalTimeSlotRawData.toSlotOrNull(): MpLocalTimeSlot? {
    val start = storedTime(from)

    return when (duration) {
        DURATION_ABSENT -> to?.let { MpLocalTimeSlot.of(from = start, to = storedTime(it)) }
        null -> null
        else -> MpLocalTimeSlot.of(from = start, duration = duration.milliseconds)
    }
}

/** A stored millisecond of the day: exactly one day is 24:00, every other value wraps (as [MpLocalTimeSerializer]). */
private fun storedTime(millis: Long): MpLocalTime = when (millis) {
    MpLocalTime.MILLIS_PER_DAY.toLong() -> MpLocalTime.EndOfDay
    else -> MpLocalTime.ofMilliSeconds(millis.mod(MpLocalTime.MILLIS_PER_DAY.toLong()).toInt())
}
