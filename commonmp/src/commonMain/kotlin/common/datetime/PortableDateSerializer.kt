package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = PortableDate::class)
object PortableDateSerializer : KSerializer<PortableDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PortableDateSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PortableDate) {
        encoder.encodeSerializableValue(
            serializer = SerializationTuple.serializer(),
            value = SerializationTuple(
                ts = value.timestamp,
                timezone = "UTC",
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): PortableDate {
        val v = decoder.decodeSerializableValue(SerializationTuple.serializer())

        return PortableDate(v.ts)
    }
}
