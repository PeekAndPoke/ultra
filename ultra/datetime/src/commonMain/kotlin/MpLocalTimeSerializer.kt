package io.peekandpoke.ultra.datetime

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
        // Still written as a Long: the wire format does not change with the Int storage.
        encoder.encodeLong(value.inWholeMilliSeconds().toLong())
    }

    override fun deserialize(decoder: Decoder): MpLocalTime {
        val v = decoder.decodeLong()

        return MpLocalTime.ofMilliSeconds(v.mod(MpLocalTime.MILLIS_PER_DAY.toLong()).toInt())
    }
}
