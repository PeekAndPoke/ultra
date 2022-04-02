package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
