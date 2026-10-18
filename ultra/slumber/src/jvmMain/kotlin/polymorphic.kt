package io.peekandpoke.ultra.slumber

import org.atteo.classindex.ClassIndex
import org.atteo.classindex.IndexSubclasses
import kotlin.reflect.KClass

/**
 * Intended as the implementation of [Polymorphic.Parent.childTypes] for a COMPANION object: it resolves
 * the class declaring that companion and returns the `KClass.indexedSubClasses` of it.
 */
// TODO(scan): swallows every Throwable into an empty set (and prints a stack trace to stderr). Two ways
//  this misfires: a Polymorphic.Parent that is not a companion has a null `declaringClass`, so `.kotlin`
//  NPEs; and a module that never ran the classindex processor simply indexes nothing. Both degrade to
//  "hierarchy has no children", which surfaces far away as an unrelated "must not be null".
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
 * To make this work, the parent class needs to be annotated with [IndexSubclasses], AND every module
 * that declares sub-classes must run the classindex annotation processor — the index is built per
 * compilation unit, so an un-processed module contributes nothing and fails silently.
 */
internal val <T : Any> KClass<T>.indexedSubClasses
    get(): Set<KClass<out T>> = ClassIndex.getSubclasses(java).map { it.kotlin }.toSet()
