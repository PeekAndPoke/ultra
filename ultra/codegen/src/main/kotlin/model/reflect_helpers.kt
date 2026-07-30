package io.peekandpoke.ultra.codegen.model

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

/**
 * Creates a [KType] for this class, filling any type parameters with `Any` projections.
 *
 * Mirrors what `TypeRef.createForKClass` does, so a class reached without type arguments (e.g. a
 * polymorphic child listed by `sealedSubclasses`) still yields a usable type.
 */
internal fun KClass<*>.createBareType(): KType = createType(
    arguments = typeParameters.map { KTypeProjection.invariant(Any::class.createType()) }
)

/**
 * True for a value class that Slumber serializes generically via its backing field.
 *
 * Mirrors `BuiltInModule.isUserValueClass` exactly: kotlin STDLIB value classes (`Duration`, `UInt`,
 * `ULong`, `Result`) are EXCLUDED, because their generic backing-field form would diverge from
 * kotlinx and Slumber deliberately fails fast on them instead. Treating them as plain aliases here
 * would emit a type for something the server cannot serialize.
 */
internal fun KClass<*>.isUserValueClass(): Boolean =
    isValue && qualifiedName?.startsWith("kotlin.") != true

/** True when this class is an enum. */
internal fun KClass<*>.isEnumClass(): Boolean = java.isEnum

/**
 * True when Slumber routes this class through `CollectionSlumberer`, i.e. it becomes a JSON array.
 *
 * Mirrors `BuiltInModule`'s two branches: `Iterable::class.java.isAssignableFrom(cls.java)` and
 * `cls.java.isArray`. Arrays need the second one because an `Array` is not an `Iterable`.
 */
internal fun KClass<*>.isArrayLike(): Boolean =
    Iterable::class.java.isAssignableFrom(java) || java.isArray

/**
 * The element type of a Kotlin primitive array class, or `null` when this is not one.
 *
 * `IntArray` is not `Array<Int>` and carries no type argument, so the element type cannot be read
 * off the [KType] and has to be looked up — the same table `CollectionAwaker` keeps.
 */
internal fun KClass<*>.primitiveArrayElementType(): KType? = when (this) {
    BooleanArray::class -> typeOf<Boolean>()
    ByteArray::class -> typeOf<Byte>()
    CharArray::class -> typeOf<Char>()
    DoubleArray::class -> typeOf<Double>()
    FloatArray::class -> typeOf<Float>()
    IntArray::class -> typeOf<Int>()
    LongArray::class -> typeOf<Long>()
    ShortArray::class -> typeOf<Short>()
    else -> null
}

/** True when Slumber routes this class through `MapSlumberer`, i.e. it becomes a JSON object. */
internal fun KClass<*>.isMapLike(): Boolean = Map::class.java.isAssignableFrom(java)

/**
 * True when this class has a primary constructor taking no parameters.
 *
 * Slumber hands these to `DataClassSlumberer` even when they are not data classes
 * (`BuiltInModule.getSlumberer`, the branch after the `isData` check).
 */
internal fun KClass<*>.hasNoArgPrimaryCtor(): Boolean =
    primaryConstructor?.parameters?.isEmpty() == true
