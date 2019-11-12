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
                cls in listOf(Any::class, Serializable::class) -> type.wrapIfNonNull(AnyAwaker)

                // Primitive types
                cls == Number::class -> type.wrapIfNonNull(NumberAwaker)
                cls == Boolean::class -> type.wrapIfNonNull(BooleanAwaker)
                cls == Byte::class -> type.wrapIfNonNull(ByteAwaker)
                cls == Char::class -> type.wrapIfNonNull(CharAwaker)
                cls == Double::class -> type.wrapIfNonNull(DoubleAwaker)
                cls == Float::class -> type.wrapIfNonNull(FloatAwaker)
                cls == Int::class -> type.wrapIfNonNull(IntAwaker)
                cls == Long::class -> type.wrapIfNonNull(LongAwaker)
                cls == Short::class -> type.wrapIfNonNull(ShortAwaker)

                // Strings
                cls == String::class -> type.wrapIfNonNull(StringAwaker)

                // Lists
                cls == Iterable::class || cls == List::class ->
                    type.wrapIfNonNull(CollectionAwaker.forList(type.arguments[0].type!!))
                cls == MutableList::class ->
                    type.wrapIfNonNull(CollectionAwaker.forMutableList(type.arguments[0].type!!))
                cls == Set::class ->
                    type.wrapIfNonNull(CollectionAwaker.forSet(type.arguments[0].type!!))
                cls == MutableSet::class ->
                    type.wrapIfNonNull(CollectionAwaker.forMutableSet(type.arguments[0].type!!))

                // Maps
                cls == Map::class ->
                    MapAwaker(type.arguments[0].type!!, type.arguments[1].type!!)

                // Data classes
                cls.isData -> type.wrapIfNonNull(DataClassAwaker(type))

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
                cls in listOf(Any::class, Serializable::class) -> type.wrapIfNonNull(AnySlumberer)

                // Primitive types
                cls == Number::class -> type.wrapIfNonNull(NumberSlumberer)
                cls == Boolean::class -> type.wrapIfNonNull(BooleanSlumberer)
                cls == Byte::class -> type.wrapIfNonNull(ByteSlumberer)
                cls == Char::class -> type.wrapIfNonNull(CharSlumberer)
                cls == Double::class -> type.wrapIfNonNull(DoubleSlumberer)
                cls == Float::class -> type.wrapIfNonNull(FloatSlumberer)
                cls == Int::class -> type.wrapIfNonNull(IntSlumberer)
                cls == Long::class -> type.wrapIfNonNull(LongSlumberer)
                cls == Short::class -> type.wrapIfNonNull(ShortSlumberer)

                // Strings
                cls == String::class -> type.wrapIfNonNull(StringSlumberer)

                // Iterables
                Iterable::class.java.isAssignableFrom(cls.java) -> type.wrapIfNonNull(CollectionSlumberer)

                // Maps
                Map::class.java.isAssignableFrom(cls.java) -> MapSlumberer

                // Data classes
                cls.isData -> type.wrapIfNonNull(DataClassSlumberer(cls))

                else -> null
            }
        }

        return null
    }
}
