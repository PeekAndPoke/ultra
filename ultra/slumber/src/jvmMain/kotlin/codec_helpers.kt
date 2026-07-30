package io.peekandpoke.ultra.slumber

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.slumber.Codec.Companion.createType
import kotlin.reflect.KClass

/** Returns the [Awaker] for the type represented by [type]. */
fun <T> Codec.getAwaker(type: TypeRef<T>): Awaker {
    return getAwaker(type.type)
}

/** Returns the [Awaker] for the reified type [T]. */
inline fun <reified T> Codec.getAwaker(): Awaker {
    return getAwaker(kType<T>().type)
}

/**
 * Deserializes [data] into an instance of [cls].
 *
 * Type arguments are approximated (`Box<T>` is awoken as `Box<Any?>`); prefer the [TypeRef] or reified
 * overload for generic classes.
 */
fun <T : Any> Codec.awake(cls: KClass<T>, data: Any?): T? {
    @Suppress("UNCHECKED_CAST")
    return awake(cls.createType(), data) as T?
}

/** Deserializes [data] into an instance of the type represented by [type]. */
fun <T> Codec.awake(type: TypeRef<T>, data: Any?): T? {
    @Suppress("UNCHECKED_CAST")
    return awake(type.type, data) as T?
}

/**
 * Deserializes [data] into an instance of the reified type [T].
 *
 * Being reified, the cast is checked here — a mismatch throws `ClassCastException`, whereas the
 * unchecked overloads above hand back a silently mistyped reference.
 */
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

/** Serializes [data], inferring the type from the runtime class (`Nothing` when null). */
// TODO(scan): `codec.slumber(Any())` recurses forever - the runtime class is Any, whose slumberer is
//  AnySlumberer, which calls back into this same lookup. Blows the stack instead of raising SlumberException.
fun Codec.slumber(data: Any?): Any? {
    val cls = when {
        data != null -> data::class
        else -> Nothing::class
    }

    return slumber(cls, data)
}

/** Serializes [data] as the given [targetType]. */
// TODO(scan): the declared return type is the SOURCE type `T?`, but slumbering yields raw data
//  (Map/List/scalar). `codec.slumber(Person::class, person)` types a Map as a Person via an unchecked cast.
fun <T : Any> Codec.slumber(targetType: KClass<T>, data: Any?): T? {
    @Suppress("UNCHECKED_CAST")
    return slumber(targetType.createType(), data) as T?
}
