package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = PortableTimezone::class)
object PortableTimezoneSerializer : KSerializer<PortableTimezone> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PortableTimezoneSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PortableTimezone) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): PortableTimezone {
        val v = decoder.decodeString()

        return PortableTimezone(v)
    }
}
