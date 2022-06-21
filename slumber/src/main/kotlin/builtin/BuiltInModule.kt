package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionSlumberer
import de.peekandpoke.ultra.slumber.builtin.collections.MapAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.MapSlumberer
import de.peekandpoke.ultra.slumber.builtin.objects.AnyAwaker
import de.peekandpoke.ultra.slumber.builtin.objects.AnySlumberer
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassCodec
import de.peekandpoke.ultra.slumber.builtin.objects.EnumCodec
import de.peekandpoke.ultra.slumber.builtin.objects.NullCodec
import de.peekandpoke.ultra.slumber.builtin.objects.ObjectInstanceCodec
import de.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicChildUtil
import de.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicParentUtil
import de.peekandpoke.ultra.slumber.builtin.primitive.BooleanAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.BooleanSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.ByteAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.ByteSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.CharAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.CharSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.DoubleAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.DoubleSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.FloatAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.FloatSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.IntAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.IntSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.LongAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.LongSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.NumberAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.NumberSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.ShortAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.ShortSlumberer
import de.peekandpoke.ultra.slumber.builtin.primitive.StringAwaker
import de.peekandpoke.ultra.slumber.builtin.primitive.StringSlumberer
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

object BuiltInModule : SlumberModule {

    @Suppress("Detekt:ComplexMethod")
    override fun getAwaker(type: KType): Awaker? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            return when {
                // Null or Nothing
                cls in listOf(Nothing::class, Unit::class) ->
                    NullCodec

                // Any type
                cls in listOf(Any::class, Serializable::class) ->
                    type.wrapIfNonNull(AnyAwaker)

                // Primitive types
                cls == Number::class ->
                    type.wrapIfNonNull(NumberAwaker)
                cls == Boolean::class ->
                    type.wrapIfNonNull(BooleanAwaker)
                cls == Byte::class ->
                    type.wrapIfNonNull(ByteAwaker)
                cls == Char::class ->
                    type.wrapIfNonNull(CharAwaker)
                cls == Double::class ->
                    type.wrapIfNonNull(DoubleAwaker)
                cls == Float::class ->
                    type.wrapIfNonNull(FloatAwaker)
                cls == Int::class ->
                    type.wrapIfNonNull(IntAwaker)
                cls == Long::class ->
                    type.wrapIfNonNull(LongAwaker)
                cls == Short::class ->
                    type.wrapIfNonNull(ShortAwaker)

                // Strings
                cls == String::class -> type.wrapIfNonNull(StringAwaker)

                // Lists
                cls == Iterable::class || cls == List::class || cls == MutableList::class ->
                    type.wrapIfNonNull(CollectionAwaker.forList(type))

                // Sets
                cls == Set::class || cls == MutableSet::class ->
                    type.wrapIfNonNull(CollectionAwaker.forSet(type))

                // Maps
                cls == Map::class || cls == MutableMap::class ->
                    type.wrapIfNonNull(MapAwaker.forMap(type))

                // Enum
                cls.java.isEnum ->
                    type.wrapIfNonNull(EnumCodec(type) as Awaker)

                // Polymorphic classes
                PolymorphicParentUtil.isPolymorphicParent(cls) ->
                    type.wrapIfNonNull(PolymorphicParentUtil.createParentAwaker(cls))

                // Singleton Object instance
                cls.objectInstance != null ->
                    type.wrapIfNonNull(ObjectInstanceCodec(cls.objectInstance!!) as Awaker)

                // Data classes
                cls.isData ->
                    type.wrapIfNonNull(DataClassCodec(type) as Awaker)

                // No param ctor (objects)
                cls.primaryConstructor != null && cls.primaryConstructor!!.parameters.isEmpty() ->
                    type.wrapIfNonNull(DataClassCodec(type) as Awaker)

                // Type cannot be handled by this module
                else -> null
            }
        }

        return null
    }

    @Suppress("Detekt:ComplexMethod")
    override fun getSlumberer(type: KType): Slumberer? {

        val cls = type.classifier
        val args = type.arguments

        if (cls is KClass<*>) {

            return when {
                // Null or Nothing
                cls in listOf(Nothing::class, Unit::class) -> NullCodec

                // Any, Object or Serializable type
                cls in listOf(Any::class, Serializable::class) ->
                    type.wrapIfNonNull(AnySlumberer)

                // Primitive types
                cls == Number::class ->
                    type.wrapIfNonNull(NumberSlumberer)
                cls == Boolean::class ->
                    type.wrapIfNonNull(BooleanSlumberer)
                cls == Byte::class ->
                    type.wrapIfNonNull(ByteSlumberer)
                cls == Char::class ->
                    type.wrapIfNonNull(CharSlumberer)
                cls == Double::class ->
                    type.wrapIfNonNull(DoubleSlumberer)
                cls == Float::class ->
                    type.wrapIfNonNull(FloatSlumberer)
                cls == Int::class ->
                    type.wrapIfNonNull(IntSlumberer)
                cls == Long::class ->
                    type.wrapIfNonNull(LongSlumberer)
                cls == Short::class ->
                    type.wrapIfNonNull(ShortSlumberer)

                // Strings
                cls == String::class ->
                    type.wrapIfNonNull(StringSlumberer)

                // Iterables
                Iterable::class.java.isAssignableFrom(cls.java) ->
                    type.wrapIfNonNull(
                        CollectionSlumberer(
                            args.getOrNull(0)?.type ?: TypeRef.Any.type
                        )
                    )

                // Maps
                Map::class.java.isAssignableFrom(cls.java) ->
                    type.wrapIfNonNull(
                        MapSlumberer(
                            args.getOrNull(0)?.type ?: TypeRef.String.type,
                            args.getOrNull(1)?.type ?: TypeRef.Any.type
                        )
                    )

                // Enum
                cls.java.isEnum ->
                    type.wrapIfNonNull(EnumCodec(type) as Slumberer)

                // Polymorphic classes
                PolymorphicParentUtil.isPolymorphicParent(cls) ->
                    type.wrapIfNonNull(PolymorphicParentUtil.createParentSlumberer(cls))

                PolymorphicChildUtil.isPolymorphicChild(cls) ->
                    type.wrapIfNonNull(PolymorphicChildUtil.createChildSlumberer(type))

                // Singleton Object instance
                cls.objectInstance != null ->
                    type.wrapIfNonNull(ObjectInstanceCodec(cls.objectInstance!!) as Slumberer)

                // Data classes
                cls.isData ->
                    type.wrapIfNonNull(DataClassCodec(type) as Slumberer)

                // No param ctor (objects)
                cls.primaryConstructor != null && cls.primaryConstructor!!.parameters.isEmpty() ->
                    type.wrapIfNonNull(DataClassCodec(type) as Slumberer)

                else -> null
            }
        }

        return null
    }
}
