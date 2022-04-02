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
@Serializer(forClass = MpLocalDateTime::class)
object MpLocalDateTimeSerializer : KSerializer<MpLocalDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpLocalDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpLocalDateTime) {
        encoder.encodeSerializableValue(
            serializer = SerializationTuple.serializer(),
            value = SerializationTuple(
                ts = value.toInstant(TimeZone.UTC).toEpochMillis(),
                timezone = "UTC",
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpLocalDateTime {
        val v = decoder.decodeSerializableValue(SerializationTuple.serializer())

        val timezone = TimeZone.of(v.timezone)

        return MpLocalDateTime(
            Instant.fromEpochMilliseconds(v.ts).toLocalDateTime(timezone)
        )
    }
}
