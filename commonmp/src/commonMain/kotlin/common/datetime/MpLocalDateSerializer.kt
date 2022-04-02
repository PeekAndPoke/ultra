package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = MpLocalDate::class)
object MpLocalDateSerializer : KSerializer<MpLocalDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpLocalDateSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpLocalDate) {
        encoder.encodeSerializableValue(
            serializer = SerializationTuple.serializer(),
            value = SerializationTuple(
                ts = value.atStartOfDay(TimeZone.UTC).toEpochMillis(),
                timezone = "UTC",
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpLocalDate {
        val v = decoder.decodeSerializableValue(SerializationTuple.serializer())

        val timezone = TimeZone.of(v.timezone)

        return MpLocalDate(
            Instant.fromEpochMilliseconds(v.ts).toLocalDateTime(timezone).date
        )
    }
}
