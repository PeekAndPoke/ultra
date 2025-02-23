package de.peekandpoke.ultra.common.reflection

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

/**
 * Creates a [TypeRef] from the given class [T]
 *
 * The given type [T] must not be generic, otherwise an exception will be thrown
 */
inline fun <reified T : Any?> kType(): TypeRef<T> {

    val type: KType = typeOf<T>()

    return TypeRef.createForKType(type = type)
}

/**
 * Creates a [TypeRef] from the given [Class]
 */
fun <T : Any> Class<T>.kType(): TypeRef<T> = kotlin.kType()

/**
 * Creates a [TypeRef] from the given [KClass]
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

/**
 * Creates a [TypeRef] for a MutableMap type with the given [KEY] and [VAL] types
 */
inline fun <reified KEY, reified VAL> kMutableMapType(): TypeRef<Map<KEY, VAL>> =
    TypeRef.createForKType(
        MutableMap::class.createType(
            arguments = listOf(
                KTypeProjection.invariant(kType<KEY>().type),
                KTypeProjection.invariant(kType<VAL>().type)
            )
        )
    )
