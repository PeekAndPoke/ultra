package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MpTimezoneSerializer : KSerializer<MpTimezone> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpTimezoneSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpTimezone) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): MpTimezone {
        val v = decoder.decodeString()

        return MpTimezone.of(v)
    }
}
