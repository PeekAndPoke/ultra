package de.peekandpoke.common.datetime

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
object SerializableDateSerializer : KSerializer<PortableDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WithCustomDefault", PrimitiveKind.STRING)

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
object SerializableDateTimeSerializer : KSerializer<PortableDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    @Serializable
    private data class V(
        val ts: Long,
        val timezone: String,
        val human: String
    )

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
