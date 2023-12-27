package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MpInstantSerializer : KSerializer<MpInstant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpInstantSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpInstant) {
        encoder.encodeSerializableValue(
            serializer = SerializationTuple.serializer(),
            value = SerializationTuple(
                ts = value.toEpochMillis(),
                timezone = "UTC",
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpInstant {
        val v = decoder.decodeSerializableValue(SerializationTuple.serializer())

        return MpInstant.fromEpochMillis(v.ts)
    }
}
