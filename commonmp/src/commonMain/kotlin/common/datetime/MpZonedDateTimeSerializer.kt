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
@Serializer(forClass = MpZonedDateTime::class)
object MpZonedDateTimeSerializer : KSerializer<MpZonedDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MpZonedDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MpZonedDateTime) {
        encoder.encodeSerializableValue(
            serializer = SerializationTuple.serializer(),
            value = SerializationTuple(
                ts = value.toInstant().toEpochMillis(),
                timezone = value.timezone.id,
                human = value.toIsoString()
            )
        )
    }

    override fun deserialize(decoder: Decoder): MpZonedDateTime {
        val v = decoder.decodeSerializableValue(SerializationTuple.serializer())

        val timezone = TimeZone.of(v.timezone)

        return MpZonedDateTime.of(
            value = MpLocalDateTime(
                Instant.fromEpochMilliseconds(v.ts).toLocalDateTime(timezone)
            ),
            timezone = timezone,
        )
    }
}
