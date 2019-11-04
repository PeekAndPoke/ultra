package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Shared
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionSlumberer
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassAwaker
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

class BuiltInModule : SlumberModule {

    override fun getAwaker(type: KType, shared: Shared): Awaker? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            return when {
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
                cls == Iterable::class -> CollectionAwaker.forList(type.arguments[0].type!!, shared)
                cls == List::class -> CollectionAwaker.forList(type.arguments[0].type!!, shared)
                cls == MutableList::class -> CollectionAwaker.forMutableList(type.arguments[0].type!!, shared)

                cls == Set::class -> CollectionAwaker.forSet(type.arguments[0].type!!, shared)
                cls == MutableSet::class -> CollectionAwaker.forMutableSet(type.arguments[0].type!!, shared)

                // Data classes
                cls.isData -> return DataClassAwaker(cls, shared)

                else -> null
            }
        }

        return null
    }

    override fun getSlumberer(type: KType, shared: Shared): Slumberer? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            return when {
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

                // Lists, Set, Collections
                Iterable::class.java.isAssignableFrom(cls.java) -> CollectionSlumberer(type.arguments[0].type!!, shared)

                // Data classes
                cls.isData -> return DataClassSlumberer(cls, shared)

                else -> null
            }
        }

        return null
    }
}
