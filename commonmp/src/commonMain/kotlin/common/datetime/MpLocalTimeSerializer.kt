package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = MpLocalTime::class)
object MpLocalTimeSerializer : KSerializer<MpLocalTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpLocalTime) {
        encoder.encodeLong(value.milliSeconds)
    }

    override fun deserialize(decoder: Decoder): MpLocalTime {
        val v = decoder.decodeLong()

        return MpLocalTime(v)
    }
}
