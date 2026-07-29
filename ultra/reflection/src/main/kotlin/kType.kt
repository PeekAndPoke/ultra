package io.peekandpoke.ultra.reflection

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

/**
 * Creates a [TypeRef] for the reified type [T], keeping its type arguments and nullability.
 */
inline fun <reified T : Any?> kType(): TypeRef<T> {

    val type: KType = typeOf<T>()

    return TypeRef.createForKType(type = type)
}

/**
 * Creates a non-null [TypeRef] from the given [Class], filling unknown type arguments with `Any`.
 */
fun <T : Any> Class<T>.kType(): TypeRef<T> = kotlin.kType()

/**
 * Creates a non-null [TypeRef] from the given [KClass], filling unknown type arguments with `Any`.
 *
 * A class carries no type arguments, so `List::class.kType()` yields `List<Any>`, not `List<T>`.
 */
fun <T : Any> KClass<T>.kType(): TypeRef<T> = TypeRef.createForKClass(cls = this, nullable = false)

/**
 * Creates a [TypeRef] for a List type of the given type
 */
inline fun <reified T> kListType(): TypeRef<List<T>> = kType<T>().list

/**
 * Creates a [TypeRef] for a Map type with the given [KEY] and [VAL] types
 */
inline fun <reified KEY, reified VAL> kMapType(): TypeRef<Map<KEY, VAL>> =
    TypeRef.createForKType(
        Map::class.createType(
            arguments = listOf(
                KTypeProjection.invariant(kType<KEY>().type),
                KTypeProjection.invariant(kType<VAL>().type)
            )
        )
    )
