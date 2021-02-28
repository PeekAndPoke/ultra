package de.peekandpoke.ultra.common.datetime

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
    val human: String
)

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = PortableDate::class)
object PortableDateSerializer : KSerializer<PortableDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(this::class.qualifiedName!!, PrimitiveKind.STRING)

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
        PrimitiveSerialDescriptor(this::class.qualifiedName!!, PrimitiveKind.STRING)

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
        PrimitiveSerialDescriptor(this::class.qualifiedName!!, PrimitiveKind.STRING)

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
        PrimitiveSerialDescriptor(this::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PortableTimezone) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): PortableTimezone {
        val v = decoder.decodeString()

        return PortableTimezone(v)
    }
}
