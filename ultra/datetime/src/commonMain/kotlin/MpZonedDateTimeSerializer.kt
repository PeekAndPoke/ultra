package io.peekandpoke.ultra.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MpZonedDateTimeSerializer : KSerializer<MpZonedDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpZonedDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpZonedDateTime) {
        encoder.encodeSerializableValue(
            serializer = MpDateTimeRawData.serializer(),
            value = MpDateTimeRawData(
                ts = value.toInstant().toEpochMillis(),
                timezone = value.timezone.id,
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpZonedDateTime {
        val v = decoder.decodeSerializableValue(MpDateTimeRawData.serializer())

        // ts is the instant: rebuilding from the local time would move the second half of a DST overlap (ULTRA-00211)
        return MpInstant.fromEpochMillis(v.ts).atZone(MpTimezone.of(v.timezone))
    }
}
