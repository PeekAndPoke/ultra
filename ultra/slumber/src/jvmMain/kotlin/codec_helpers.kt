package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.Codec.Companion.createType
import kotlin.reflect.KClass

/** Returns the [Awaker] for the type represented by [type]. */
fun <T> Codec.getAwaker(type: TypeRef<T>): Awaker {
    return getAwaker(type.type)
}

/** Returns the [Awaker] for the reified type [T]. */
inline fun <reified T> Codec.getAwaker(): Awaker {
    return getAwaker(kType<T>().type)
}

/** Deserializes [data] into an instance of [cls]. */
fun <T : Any> Codec.awake(cls: KClass<T>, data: Any?): T? {
    @Suppress("UNCHECKED_CAST")
    return awake(cls.createType(), data) as T?
}

/** Deserializes [data] into an instance of the type represented by [type]. */
fun <T> Codec.awake(type: TypeRef<T>, data: Any?): T? {
    @Suppress("UNCHECKED_CAST")
    return awake(type.type, data) as T?
}

/** Deserializes [data] into an instance of the reified type [T]. */
inline fun <reified T> Codec.awake(data: Any?): T? {
    return awake(kType<T>().type, data) as T?
}

/** Returns the [Slumberer] for the type represented by [type]. */
fun <T> Codec.getSlumberer(type: TypeRef<T>): Slumberer {
    return getSlumberer(type.type)
}

/** Returns the [Slumberer] for the reified type [T]. */
inline fun <reified T> Codec.getSlumberer(): Slumberer {
    return getSlumberer(kType<T>().type)
}

/** Serializes [data], inferring the type from the runtime class. */
fun Codec.slumber(data: Any?): Any? {
    val cls = when {
        data != null -> data::class
        else -> Nothing::class
    }

    return slumber(cls, data)
}

/** Serializes [data] as the given [targetType]. */
fun <T : Any> Codec.slumber(targetType: KClass<T>, data: Any?): T? {
    @Suppress("UNCHECKED_CAST")
    return slumber(targetType.createType(), data) as T?
}
