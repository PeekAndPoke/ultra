package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Checks if the class is primitive or a string
 */
internal fun KClass<*>.isPrimitiveOrString() = java.isPrimitive || this == String::class

/**
 * Checks if the class is the [InjectionContext] type
 */
internal fun KClass<*>.isInjectionContext() = this == InjectionContext::class

/**
 * Checks if the class is suitable as a service
 */
internal fun KClass<*>.isServiceType() = !isPrimitiveOrString()

/**
 * Checks if the type is a [List] and if the inner type is suitable as service
 */
internal fun KType.isListType() =
    classifier == List::class && getInnerClass().isServiceType()

/**
 * Checks if the type is a [Lookup] and if the inner type is suitable as service
 */
internal fun KType.isLookupType() =
    classifier == Lookup::class && getInnerClass().isServiceType()

/**
 * Checks if the type is a [Lazy] and if the inner type is suitable as service
 */
internal fun KType.isLazyServiceType() =
    classifier == Lazy::class && getInnerClass().isServiceType()

/**
 * Checks if the type is [Lazy] and if the inner type is suitable as a list of services
 */
internal fun KType.isLazyListType() =
    classifier == Lazy::class && arguments[0].type!!.isListType()

/**
 * Gets the first type parameter as class
 */
internal fun KType.getInnerClass() =
    arguments[0].type!!.classifier as KClass<*>

/**
 * Gets the first type parameter of the first type parameter as class
 */
internal fun KType.getInnerInnerClass() =
    arguments[0].type!!.arguments[0].type!!.classifier!! as KClass<*>
