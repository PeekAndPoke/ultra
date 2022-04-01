package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
private data class V(
    val ts: Long,
    val timezone: String,
    val human: String? = ""
)

// Mp*Serializer //////////////////////////////////////////////////////////////////////////////////////////

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = MpInstant::class)
object MpInstantSerializer : KSerializer<MpInstant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpInstantSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpInstant) {
        encoder.encodeSerializableValue(
            serializer = V.serializer(),
            value = V(
                ts = value.toEpochMillis(),
                timezone = "UTC",
                human = value.toString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpInstant {
        val v = decoder.decodeSerializableValue(V.serializer())

        return MpInstant.fromEpochMillis(v.ts)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = MpLocalDate::class)
object MpLocalDateSerializer : KSerializer<MpLocalDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpLocalDateSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpLocalDate) {
        encoder.encodeSerializableValue(
            serializer = V.serializer(),
            value = V(
                ts = value.value.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                timezone = "UTC",
                human = value.toString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpLocalDate {
        val v = decoder.decodeSerializableValue(V.serializer())

        return MpLocalDate(
            Instant.fromEpochMilliseconds(v.ts).toLocalDateTime(TimeZone.UTC).date
        )
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = MpLocalDateTime::class)
object MpLocalDateTimeSerializer : KSerializer<MpLocalDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpLocalDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpLocalDateTime) {
        encoder.encodeSerializableValue(
            serializer = V.serializer(),
            value = V(
                ts = value.toInstant(TimeZone.UTC).toEpochMillis(),
                timezone = "UTC",
                human = value.toString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpLocalDateTime {
        val v = decoder.decodeSerializableValue(V.serializer())

        return MpLocalDateTime(
            Instant.fromEpochMilliseconds(v.ts).toLocalDateTime(TimeZone.UTC)
        )
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = MpZonedDateTime::class)
object MpZonedDateTimeSerializer : KSerializer<MpZonedDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpZonedDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpZonedDateTime) {
        encoder.encodeSerializableValue(
            serializer = V.serializer(),
            value = V(
                ts = value.toInstant().toEpochMillis(),
                timezone = value.timezone.id,
                human = value.toString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpZonedDateTime {
        val v = decoder.decodeSerializableValue(V.serializer())

        val timezone = TimeZone.of(v.timezone)

        return MpZonedDateTime.of(
            value = Instant.fromEpochMilliseconds(v.ts).toLocalDateTime(timezone),
            timezone = timezone,
        )
    }
}

// Portable*Serializer ////////////////////////////////////////////////////////////////////////////////////

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = PortableDate::class)
object PortableDateSerializer : KSerializer<PortableDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PortableDateSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PortableDate) {
        encoder.encodeSerializableValue(
            serializer = V.serializer(),
            value = V(
                ts = value.timestamp,
                timezone = "UTC",
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): PortableDate {
        val v = decoder.decodeSerializableValue(V.serializer())

        return PortableDate(v.ts)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = PortableDateTime::class)
object PortableDateTimeSerializer : KSerializer<PortableDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PortableDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PortableDateTime) {
        encoder.encodeSerializableValue(
            serializer = V.serializer(),
            value = V(
                ts = value.timestamp,
                timezone = "UTC",
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): PortableDateTime {
        val v = decoder.decodeSerializableValue(V.serializer())

        return PortableDateTime(v.ts)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = PortableTime::class)
object PortableTimeSerializer : KSerializer<PortableTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PortableTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PortableTime) {
        encoder.encodeLong(value.milliSeconds)
    }

    override fun deserialize(decoder: Decoder): PortableTime {
        val v = decoder.decodeLong()

        return PortableTime(v)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = PortableTimezone::class)
object PortableTimezoneSerializer : KSerializer<PortableTimezone> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PortableTimezoneSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PortableTimezone) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): PortableTimezone {
        val v = decoder.decodeString()

        return PortableTimezone(v)
    }
}
