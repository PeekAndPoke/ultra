package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionSlumberer
import de.peekandpoke.ultra.slumber.builtin.collections.MapAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.MapSlumberer
import de.peekandpoke.ultra.slumber.builtin.objects.AnyCodec
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassAwaker
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
import de.peekandpoke.ultra.slumber.builtin.objects.NothingCodec
import de.peekandpoke.ultra.slumber.builtin.primitive.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

object BuiltInModule : SlumberModule {

    override fun getAwaker(type: KType): Awaker? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            return when {
                // Null or Nothing
                cls in listOf(Nothing::class, Unit::class) -> NothingCodec
                // Any type
                cls == Any::class -> AnyCodec

                // Primitive types
                cls == Boolean::class -> BooleanCodec
                cls == Byte::class -> ByteCodec
                cls == Char::class -> CharCodec
                cls == Double::class -> DoubleCodec
                cls == Float::class -> FloatCodec
                cls == Int::class -> IntCodec
                cls == Long::class -> LongCodec
                cls == Short::class -> ShortCodec

                // Strings
                cls == String::class -> StringCodec

                // Lists
                cls == Iterable::class || cls == List::class ->
                    CollectionAwaker.forList(type.arguments[0].type!!)
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
                cls.isData -> DataClassAwaker(type)

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
                cls in listOf(Nothing::class, Unit::class) -> NothingCodec
                // Any type
                cls == Any::class -> AnyCodec
                // Primitive types
                cls == Boolean::class -> BooleanCodec
                cls == Byte::class -> ByteCodec
                cls == Char::class -> CharCodec
                cls == Double::class -> DoubleCodec
                cls == Float::class -> FloatCodec
                cls == Int::class -> IntCodec
                cls == Long::class -> LongCodec
                cls == Short::class -> ShortCodec

                // Strings
                cls == String::class -> StringCodec

                // Iterables
                Iterable::class.java.isAssignableFrom(cls.java) -> CollectionSlumberer

                // Maps
                Map::class.java.isAssignableFrom(cls.java) -> MapSlumberer

                // Data classes
                cls.isData -> DataClassSlumberer(cls)

                else -> null
            }
        }

        return null
    }
}
