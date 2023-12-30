package de.peekandpoke.ultra.slumber

import org.atteo.classindex.ClassIndex
import org.atteo.classindex.IndexSubclasses
import kotlin.reflect.KClass

@Suppress("Detekt.TooGenericExceptionCaught")
fun Polymorphic.Parent.indexedSubClasses() = try {
    this::class.java.declaringClass.kotlin.indexedSubClasses
} catch (e: Throwable) {
    e.printStackTrace()
    emptySet()
}

/**
 * A set of sub-classes indexed by [ClassIndex]
 *
 * To make this work, the parent class needs to be annotated with [IndexSubclasses]
 */
internal val <T : Any> KClass<T>.indexedSubClasses
    get(): Set<KClass<out T>> = ClassIndex.getSubclasses(java).map { it.kotlin }.toSet()
