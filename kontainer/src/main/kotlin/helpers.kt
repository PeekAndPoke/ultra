package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal fun isPrimitive(cls: KClass<*>) = cls.java.isPrimitive || cls == String::class

internal fun isServiceType(cls: KClass<*>) = !isPrimitive(cls)

internal fun isListType(type: KType) =
    type.classifier == List::class && isServiceType(type.arguments[0].type!!.classifier as KClass<*>)

internal fun isLookupType(type: KType) =
    type.classifier == Lookup::class && isServiceType(type.arguments[0].type!!.classifier as KClass<*>)

internal fun isLazyServiceType(type: KType) =
    type.classifier == Lazy::class && isServiceType(type.arguments[0].type!!.classifier as KClass<*>)

internal fun isLazyListType(type: KType) =
    type.classifier == Lazy::class && isListType(type.arguments[0].type!!)

internal fun getInnerClass(type: KType) =
    type.arguments[0].type!!.classifier as KClass<*>

internal fun getInnerInnerClass(type: KType) =
    type.arguments[0].type!!.arguments[0].type!!.classifier!! as KClass<*>
