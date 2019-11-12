package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionSlumberer
import de.peekandpoke.ultra.slumber.builtin.collections.MapAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.MapSlumberer
import de.peekandpoke.ultra.slumber.builtin.objects.*
import de.peekandpoke.ultra.slumber.builtin.primitive.*
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType

object BuiltInModule : SlumberModule {

    override fun getAwaker(type: KType): Awaker? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            return when {
                // Null or Nothing
                cls in listOf(Nothing::class, Unit::class) -> NullCodec

                // Any type
                cls in listOf(Any::class, Serializable::class) ->
                    if (type.isMarkedNullable) NullableAnyAwaker else NonNullAnyAwaker

                // Primitive types
                cls == Number::class ->
                    if (type.isMarkedNullable) NullableNumberAwaker else NonNullNumberAwaker
                cls == Boolean::class ->
                    if (type.isMarkedNullable) NullableBooleanAwaker else NonNullBooleanAwaker
                cls == Byte::class ->
                    if (type.isMarkedNullable) NullableByteAwaker else NonNullByteAwaker
                cls == Char::class ->
                    if (type.isMarkedNullable) NullableCharAwaker else NonNullCharAwaker
                cls == Double::class ->
                    if (type.isMarkedNullable) NullableDoubleAwaker else NonNullDoubleAwaker
                cls == Float::class ->
                    if (type.isMarkedNullable) NullableFloatAwaker else NonNullFloatAwaker
                cls == Int::class ->
                    if (type.isMarkedNullable) NullableIntAwaker else NonNullIntAwaker
                cls == Long::class ->
                    if (type.isMarkedNullable) NullableLongAwaker else NonNullLongAwaker
                cls == Short::class ->
                    if (type.isMarkedNullable) NullableShortAwaker else NonNullShortAwaker

                // Strings
                cls == String::class ->
                    if (type.isMarkedNullable) NullableStringAwaker else NonNullStringAwaker

                // Lists
                cls == Iterable::class || cls == List::class -> CollectionAwaker.forList(type)
                cls == MutableList::class ->
                    CollectionAwaker.forMutableList(type.arguments[0].type!!)
                cls == Set::class ->
                    CollectionAwaker.forSet(type.arguments[0].type!!)
                cls == MutableSet::class ->
                    CollectionAwaker.forMutableSet(type.arguments[0].type!!)

                // Maps
                cls == Map::class ->
                    MapAwaker(type.arguments[0].type!!, type.arguments[1].type!!)

                // Data classes
                cls.isData ->
                    if (type.isMarkedNullable) DataClassAwaker(type) else NonNullAwaker(DataClassAwaker(type))

                else -> null
            }
        }

        return null
    }

    override fun getSlumberer(type: KType): Slumberer? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            return when {
                // Null or Nothing
                cls in listOf(Nothing::class, Unit::class) -> NullCodec

                // Any, Object or Serializable type
                cls in listOf(Any::class, Serializable::class) ->
                    if (type.isMarkedNullable) NullableAnySlumberer else NonNullAnySlumberer

                // Primitive types
                cls == Number::class ->
                    if (type.isMarkedNullable) NullableNumberSlumberer else NonNullNumberSlumberer
                cls == Boolean::class ->
                    if (type.isMarkedNullable) NullableBooleanSlumberer else NonNullBooleanSlumberer
                cls == Byte::class ->
                    if (type.isMarkedNullable) NullableByteSlumberer else NonNullByteSlumberer
                cls == Char::class ->
                    if (type.isMarkedNullable) NullableCharSlumberer else NonNullCharSlumberer
                cls == Double::class ->
                    if (type.isMarkedNullable) NullableDoubleSlumberer else NonNullDoubleSlumberer
                cls == Float::class ->
                    if (type.isMarkedNullable) NullableFloatSlumberer else NonNullFloatSlumberer
                cls == Int::class ->
                    if (type.isMarkedNullable) NullableIntSlumberer else NonNullIntSlumberer
                cls == Long::class ->
                    if (type.isMarkedNullable) NullableLongSlumberer else NonNullLongSlumberer
                cls == Short::class ->
                    if (type.isMarkedNullable) NullableShortSlumberer else NonNullShortSlumberer

                // Strings
                cls == String::class ->
                    if (type.isMarkedNullable) NullableStringSlumberer else NonNullStringSlumberer

                // Iterables
                Iterable::class.java.isAssignableFrom(cls.java) ->
                    if (type.isMarkedNullable) CollectionSlumberer else NonNullSlumberer(CollectionSlumberer)

                // Maps
                Map::class.java.isAssignableFrom(cls.java) -> MapSlumberer

                // Data classes
                cls.isData ->
                    if (type.isMarkedNullable) DataClassSlumberer(cls) else NonNullSlumberer(DataClassSlumberer(cls))

                else -> null
            }
        }

        return null
    }
}
