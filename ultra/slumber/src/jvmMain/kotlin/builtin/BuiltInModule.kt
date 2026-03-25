package io.peekandpoke.ultra.slumber.builtin

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.SlumberModule
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.collections.CollectionAwaker
import io.peekandpoke.ultra.slumber.builtin.collections.CollectionSlumberer
import io.peekandpoke.ultra.slumber.builtin.collections.MapAwaker
import io.peekandpoke.ultra.slumber.builtin.collections.MapSlumberer
import io.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonCodec
import io.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonNullCodec
import io.peekandpoke.ultra.slumber.builtin.objects.AnyAwaker
import io.peekandpoke.ultra.slumber.builtin.objects.AnySlumberer
import io.peekandpoke.ultra.slumber.builtin.objects.DataClassAwaker
import io.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
import io.peekandpoke.ultra.slumber.builtin.objects.EnumCodec
import io.peekandpoke.ultra.slumber.builtin.objects.NullCodec
import io.peekandpoke.ultra.slumber.builtin.objects.ObjectInstanceCodec
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicChildUtil
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicParentUtil
import io.peekandpoke.ultra.slumber.builtin.primitive.BooleanAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.BooleanSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.ByteAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.ByteSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.CharAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.CharSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.DoubleAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.DoubleSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.FloatAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.FloatSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.IntAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.IntSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.LongAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.LongSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.NumberAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.NumberSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.ShortAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.ShortSlumberer
import io.peekandpoke.ultra.slumber.builtin.primitive.StringAwaker
import io.peekandpoke.ultra.slumber.builtin.primitive.StringSlumberer
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

/**
 * Default [SlumberModule] that provides awakers and slumberers for all built-in types:
 * primitives, strings, collections, maps, enums, data classes, object singletons,
 * polymorphic hierarchies, and KotlinX JSON elements.
 */
object BuiltInModule : SlumberModule {

    @Suppress("Detekt:ComplexMethod")
    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            val primaryCtor = cls.primaryConstructor

            return when {
                // Null or Nothing
                cls in listOf(Nothing::class, Unit::class) -> NullCodec

                else -> when {
                    // Any type
                    cls in listOf(Any::class, Serializable::class) -> AnyAwaker
                    // Primitive types
                    cls == Number::class -> NumberAwaker
                    cls == Boolean::class -> BooleanAwaker
                    cls == Byte::class -> ByteAwaker
                    cls == Char::class -> CharAwaker
                    cls == Double::class -> DoubleAwaker
                    cls == Float::class -> FloatAwaker
                    cls == Int::class -> IntAwaker
                    cls == Long::class -> LongAwaker
                    cls == Short::class -> ShortAwaker
                    cls == String::class -> StringAwaker
                    // KotlinX Json
                    KotlinXJsonNullCodec.appliesTo(cls) -> KotlinXJsonNullCodec as Awaker
                    KotlinXJsonCodec.appliesTo(cls) -> KotlinXJsonCodec as Awaker
                    // Lists
                    cls == Iterable::class || cls == List::class || cls == MutableList::class ->
                        CollectionAwaker.forList(type)
                    // Sets
                    cls == Set::class || cls == MutableSet::class -> CollectionAwaker.forSet(type)
                    // Maps
                    cls == Map::class || cls == MutableMap::class -> MapAwaker.forMap(type)
                    // Enum
                    cls.java.isEnum -> EnumCodec(type) as Awaker
                    // Polymorphic classes
                    PolymorphicParentUtil.isPolymorphicParent(cls) -> PolymorphicParentUtil.createParentAwaker(cls)
                    // Singleton Object instance
                    cls.objectInstance != null -> ObjectInstanceCodec(cls.objectInstance!!) as Awaker
                    // Data classes
                    cls.isData -> DataClassAwaker(type)
                    // No param ctor (objects)
                    primaryCtor != null && primaryCtor.parameters.isEmpty() -> DataClassAwaker(type)
                    // Type cannot be handled by this module
                    else -> null
                }?.let {
                    type.wrapIfNonNull(it)
                }
            }
        }

        return null
    }

    @Suppress("Detekt:ComplexMethod")
    override fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            val primaryCtor = cls.primaryConstructor

            return when {
                // Null or Nothing
                cls in listOf(Nothing::class, Unit::class) -> NullCodec

                // we do not wrap JsonNull with wrapIfNonNull
                KotlinXJsonNullCodec.appliesTo(cls) -> KotlinXJsonNullCodec as Slumberer

                else -> when {
                    // Any, Object or Serializable type
                    cls in listOf(Any::class, Serializable::class) -> AnySlumberer
                    // Primitive types
                    cls == Number::class -> NumberSlumberer
                    cls == Boolean::class -> BooleanSlumberer
                    cls == Byte::class -> ByteSlumberer
                    cls == Char::class -> CharSlumberer
                    cls == Double::class -> DoubleSlumberer
                    cls == Float::class -> FloatSlumberer
                    cls == Int::class -> IntSlumberer
                    cls == Long::class -> LongSlumberer
                    cls == Short::class -> ShortSlumberer
                    cls == String::class -> StringSlumberer
                    // KotlinX Json
                    KotlinXJsonCodec.appliesTo(cls) -> KotlinXJsonCodec as Slumberer
                    // Iterables
                    Iterable::class.java.isAssignableFrom(cls.java) -> CollectionSlumberer
                    // Maps
                    Map::class.java.isAssignableFrom(cls.java) -> MapSlumberer
                    // Enum
                    cls.java.isEnum -> EnumCodec(type) as Slumberer
                    // Polymorphic classes
                    PolymorphicParentUtil.isPolymorphicParent(cls) ->
                        PolymorphicParentUtil.createParentSlumberer(cls)

                    PolymorphicChildUtil.isPolymorphicChild(cls) ->
                        PolymorphicChildUtil.createChildSlumberer(type, attributes)
                    // Singleton Object instance
                    cls.objectInstance != null -> ObjectInstanceCodec(cls.objectInstance!!) as Slumberer
                    // Data classes
                    cls.isData -> DataClassSlumberer(type, attributes)
                    // No param ctor (objects)
                    primaryCtor != null && primaryCtor.parameters.isEmpty() ->
                        DataClassSlumberer(type, attributes)
                    // Type cannot be handled by this module
                    else -> null
                }?.let {
                    type.wrapIfNonNull(it)
                }
            }
        }

        return null
    }
}
