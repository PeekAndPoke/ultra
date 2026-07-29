package io.peekandpoke.ultra.codegen.model

import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

/**
 * Creates a [KType] for this class, filling any type parameters with `Any` projections.
 *
 * Mirrors what `TypeRef.createForKClass` does, so a class reached without type arguments (e.g. a
 * polymorphic child listed by `sealedSubclasses`) still yields a usable type.
 */
internal fun KClass<*>.createBareType(): KType = createType(
    arguments = typeParameters.map { KTypeProjection.invariant(Any::class.createType()) }
)

/** True when this class is a Kotlin `@JvmInline value class`. */
internal fun KClass<*>.isValueClass(): Boolean = isValue

/** True when this class is an enum. */
internal fun KClass<*>.isEnumClass(): Boolean = java.isEnum

/** True when this class is one of the collection shapes the emitter maps to a TypeScript builtin. */
internal fun KClass<*>.isCollectionLike(): Boolean =
    isSubclassOf(Collection::class) || isSubclassOf(Map::class) || java.isArray
