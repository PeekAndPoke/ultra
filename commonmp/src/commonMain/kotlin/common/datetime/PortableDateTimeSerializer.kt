package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = PortableDateTime::class)
object PortableDateTimeSerializer : KSerializer<PortableDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PortableDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PortableDateTime) {
        encoder.encodeSerializableValue(
            serializer = SerializationTuple.serializer(),
            value = SerializationTuple(
                ts = value.timestamp,
                timezone = "UTC",
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): PortableDateTime {
        val v = decoder.decodeSerializableValue(SerializationTuple.serializer())

        return PortableDateTime(v.ts)
    }
}
