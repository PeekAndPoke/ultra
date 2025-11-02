package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.CollectionSlumberer
import de.peekandpoke.ultra.slumber.builtin.collections.MapAwaker
import de.peekandpoke.ultra.slumber.builtin.collections.MapSlumberer
import de.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonCodec
import de.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonNullCodec
import de.peekandpoke.ultra.slumber.builtin.objects.AnyAwaker
import de.peekandpoke.ultra.slumber.builtin.objects.AnySlumberer
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassAwaker
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
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
