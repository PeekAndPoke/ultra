package io.peekandpoke.ultra.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MpLocalDateSerializer : KSerializer<MpLocalDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpLocalDateSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpLocalDate) {
        encoder.encodeSerializableValue(
            serializer = MpDateTimeRawData.serializer(),
            value = MpDateTimeRawData(
                ts = value.atStartOfDay(MpTimezone.UTC).toEpochMillis(),
                timezone = "UTC",
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpLocalDate {
        val v = decoder.decodeSerializableValue(MpDateTimeRawData.serializer())

        val timezone = MpTimezone.of(v.timezone)

        return MpInstant.fromEpochMillis(v.ts).atZone(timezone).toLocalDate()
    }
}
