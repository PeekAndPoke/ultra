package de.peekandpoke.ktorfx.rest.codec

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import kotlin.reflect.KType

inline fun <reified T> RestCodec.deserialize(content: Any): T? = deserialize(kType(), content)

/**
 * RestCodec definition.
 *
 * This codec is used for serializing / deserializing api models.
 */
interface RestCodec {

    fun serialize(content: Any?): String?

    fun serializePretty(content: Any?): String?

    fun serialize(asType: KType, content: Any?): String?

    fun serializePretty(asType: KType, content: Any?): String?

    fun deserialize(asType: KType, content: Any?): Any?

    @Suppress("UNCHECKED_CAST")
    fun <T> deserialize(asType: TypeRef<T>, content: Any?): T? = deserialize(asType.type, content) as? T
}
