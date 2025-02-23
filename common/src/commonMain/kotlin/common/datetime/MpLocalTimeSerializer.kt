package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MpLocalTimeSerializer : KSerializer<MpLocalTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpLocalTime) {
        encoder.encodeLong(value.inWholeMilliSeconds())
    }

    override fun deserialize(decoder: Decoder): MpLocalTime {
        val v = decoder.decodeLong()

        return MpLocalTime.ofMilliSeconds(v)
    }
}
