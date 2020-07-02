package de.peekandpoke.common.datetime

import kotlinx.serialization.*

@Serializable
private data class V(
    val ts: Long,
    val timezone: String,
    val human: String
)

@Serializer(forClass = PortableDate::class)
object SerializableDateSerializer : KSerializer<PortableDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    @ImplicitReflectionSerializer
    override fun serialize(encoder: Encoder, value: PortableDate) {
        encoder.encode(
            strategy = V.serializer(),
            value = V(
                ts = value.timestamp,
                timezone = "UTC",
                human = ""
            )
        )
    }

    override fun deserialize(decoder: Decoder): PortableDate {
        val v = decoder.decode(V.serializer())

        return PortableDate(v.ts)
    }
}

@Serializer(forClass = PortableDateTime::class)
object SerializableDateTimeSerializer : KSerializer<PortableDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    @Serializable
    private data class V(
        val ts: Long,
        val timezone: String,
        val human: String
    )

    @ImplicitReflectionSerializer
    override fun serialize(encoder: Encoder, value: PortableDateTime) {
        encoder.encode(
            strategy = V.serializer(),
            value = V(
                ts = value.timestamp,
                timezone = "UTC",
                human = ""
            )
        )
    }

    override fun deserialize(decoder: Decoder): PortableDateTime {
        val v = decoder.decode(V.serializer())

        return PortableDateTime(v.ts)
    }
}
