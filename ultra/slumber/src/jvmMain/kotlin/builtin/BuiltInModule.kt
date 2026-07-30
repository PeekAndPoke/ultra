package io.peekandpoke.ultra.slumber.builtin

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.SlumberModule
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.collections.CollectionAwaker
import io.peekandpoke.ultra.slumber.builtin.collections.CollectionSlumberer
import io.peekandpoke.ultra.slumber.builtin.collections.MapAwaker
import io.peekandpoke.ultra.slumber.builtin.collections.MapSlumberer
import io.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonArrayCodec
import io.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonElementCodec
import io.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonNullCodec
import io.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonObjectCodec
import io.peekandpoke.ultra.slumber.builtin.kotlinx.KotlinXJsonPrimitiveCodec
import io.peekandpoke.ultra.slumber.builtin.objects.AnyAwaker
import io.peekandpoke.ultra.slumber.builtin.objects.AnySlumberer
import io.peekandpoke.ultra.slumber.builtin.objects.DataClassAwaker
import io.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
import io.peekandpoke.ultra.slumber.builtin.objects.EnumCodec
import io.peekandpoke.ultra.slumber.builtin.objects.NullCodec
import io.peekandpoke.ultra.slumber.builtin.objects.ObjectInstanceCodec
import io.peekandpoke.ultra.slumber.builtin.objects.ValueClassAwaker
import io.peekandpoke.ultra.slumber.builtin.objects.ValueClassSlumberer
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
 * A `@JvmInline value class` we serialize generically via its backing field — EXCLUDING kotlin STDLIB
 * value classes (`Duration`, `UInt`/`ULong`/…, `Result`), whose generic backing-field serialization
 * would diverge from kotlinx (`Duration` -> a packed `Long`, `UInt` -> a signed `Int`) and is fragile
 * across Kotlin versions, so they fail fast here (as before) or must get a dedicated codec.
 *
 * ONLY the kotlin stdlib (`kotlin.*`) is special-cased; a THIRD-PARTY value class (e.g. `kotlinx.*`) is
 * treated as generic (backing-field). That is correct for a plain wrapper, but a library value class
 * whose serialized form differs from its backing field must get its own `SlumberModule` — as the
 * kotlinx datetime/json types already do — rather than rely on this generic path.
 */
private fun KClass<*>.isUserValueClass(): Boolean =
    isValue && qualifiedName?.startsWith("kotlin.") != true

/**
 * Default [SlumberModule] that provides awakers and slumberers for all built-in types:
 * primitives, strings, collections, maps, enums, data classes, object singletons,
 * polymorphic hierarchies, and KotlinX JSON elements.
 */
object BuiltInModule : SlumberModule {

    /**
     * Resolves an [Awaker] for the DECLARED [type] — the type written in the source, not the runtime
     * class of any value.
     *
     * Branch order is load-bearing: [KotlinXJsonNullCodec] must stay ahead of
     * [KotlinXJsonPrimitiveCodec] (`JsonNull` IS a `JsonPrimitive`), and every KotlinX branch ahead of
     * the polymorphic-parent branch (`JsonElement` and `JsonPrimitive` are sealed classes). Only the
     * [NullCodec] branch skips `wrapIfNonNull`; everything else is wrapped when the type is non-nullable.
     */
    @Suppress("Detekt:ComplexMethod")
    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            // TODO(scan): computed for EVERY class although only the last branch reads it. Kotlin
            //  reflection throws KotlinReflectionInternalError (an Error, not an Exception) for
            //  synthetic classes such as lambdas, so this masks the clean "no known way to ..." error.
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
                    KotlinXJsonObjectCodec.appliesTo(cls) -> KotlinXJsonObjectCodec as Awaker
                    KotlinXJsonArrayCodec.appliesTo(cls) -> KotlinXJsonArrayCodec as Awaker
                    KotlinXJsonPrimitiveCodec.appliesTo(cls) -> KotlinXJsonPrimitiveCodec as Awaker
                    KotlinXJsonElementCodec.appliesTo(cls) -> KotlinXJsonElementCodec as Awaker
                    // @JvmInline value classes — awake the underlying scalar + construct via ctor.
                    cls.isUserValueClass() -> ValueClassAwaker(type)
                    // Lists
                    // TODO(scan): exact-class match, unlike the slumberer's isAssignableFrom -- a field
                    //  declared `Collection<T>`, `ArrayList<T>` or `LinkedHashSet<T>` slumbers fine but
                    //  has no awaker. Also `MutableList::class` IS `List::class` on the JVM (same for
                    //  Set/Map below), so those operands never fire.
                    cls == Iterable::class || cls == List::class || cls == MutableList::class ->
                        CollectionAwaker.forList(type)
                    // Sets
                    cls == Set::class || cls == MutableSet::class -> CollectionAwaker.forSet(type)
                    // Arrays — Array<T> and the eight primitive arrays. Not covered by the branches
                    // above: an Array is not an Iterable, and IntArray & co. are not Array<T> either.
                    CollectionAwaker.isArrayClass(cls) -> CollectionAwaker.forArray(type)
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

    /**
     * Resolves a [Slumberer] for [type].
     *
     * Unlike the awaker side this is usually asked about a RUNTIME class: `Codec.slumber(data, context)`
     * derives the type from `data::class`, so the declared type of a field is not consulted.
     * Consequently the collection and map branches test assignability rather than class identity.
     *
     * Branch order is load-bearing in the same way as in [getAwaker]; additionally the KotlinX branches
     * must precede the `Iterable`/`Map` ones, because `JsonArray` is a `List` and `JsonObject` is a `Map`.
     */
    @Suppress("Detekt:ComplexMethod")
    override fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer? {

        val cls = type.classifier

        if (cls is KClass<*>) {

            // TODO(scan): see getAwaker -- eagerly computed for every class, only the last branch uses it.
            val primaryCtor = cls.primaryConstructor

            return when {
                // Null or Nothing
                cls in listOf(Nothing::class, Unit::class) -> NullCodec

                // we do not wrap JsonNull with wrapIfNonNull
                KotlinXJsonNullCodec.appliesTo(cls) -> KotlinXJsonNullCodec as Slumberer

                // @JvmInline value classes — slumber the underlying scalar via its inner codec.
                // NOT wrapped with wrapIfNonNull: reflection boxes a NULL nullable-value-class field as
                // Box(null) (a non-null box), which must slumber to null, not throw a non-null error.
                // (Trade-off: a root-level slumber of a non-null value-class type with a null value
                // returns null instead of reporting a non-null error — a degenerate, no-round-trip case.)
                // TODO(scan): sits ABOVE the polymorphic-child branch, so a `@JvmInline value class`
                //  implementing a sealed interface is emitted as a bare scalar with no discriminator and
                //  can no longer be awoken through its parent. Before value-class support it went through
                //  PolymorphicChildSlumberer + DataClassSlumberer and carried the discriminator.
                cls.isUserValueClass() -> ValueClassSlumberer(type)

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
                    KotlinXJsonObjectCodec.appliesTo(cls) -> KotlinXJsonObjectCodec as Slumberer
                    KotlinXJsonArrayCodec.appliesTo(cls) -> KotlinXJsonArrayCodec as Slumberer
                    KotlinXJsonPrimitiveCodec.appliesTo(cls) -> KotlinXJsonPrimitiveCodec as Slumberer
                    KotlinXJsonElementCodec.appliesTo(cls) -> KotlinXJsonElementCodec as Slumberer
                    // Iterables
                    // TODO(scan): assignability, and ahead of the data-class and polymorphic-child
                    //  branches -- a `data class Page<T>(...) : Iterable<T>` is emitted as a bare list,
                    //  dropping its other fields, while its awaker (exact-class match) expects a map.
                    Iterable::class.java.isAssignableFrom(cls.java) -> CollectionSlumberer
                    // Arrays — an Array is not an Iterable, so it needs its own branch. Handled by the
                    // same slumberer, which reads any array shape element-wise.
                    cls.java.isArray -> CollectionSlumberer
                    // Maps
                    Map::class.java.isAssignableFrom(cls.java) -> MapSlumberer
                    // Enum
                    // TODO(scan): `Class.isEnum()` is FALSE for a specialized enum constant (an entry
                    //  with a body compiles to a subclass of the enum), and this side dispatches on
                    //  `data::class` -- so `enum class E { A { ... } }` never reaches this branch.
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
