package de.peekandpoke.ultra.slumber

import com.github.matfax.klassindex.IndexSubclasses
import com.github.matfax.klassindex.KlassIndex
import de.peekandpoke.ultra.NoCachedKlassIndex
import kotlin.reflect.KClass

@Suppress("Detekt.TooGenericExceptionCaught")
fun Polymorphic.Parent.indexedSubClasses() = try {
    this::class.java.declaringClass.kotlin.indexedSubClasses
} catch (e: Throwable) {
    e.printStackTrace()
    emptySet()
}

/**
 * A set of sub-classes indexed by [KlassIndex]
 *
 * To make this work, the parent class needs to be annotated with [IndexSubclasses]
 */
internal val <T : Any> KClass<T>.indexedSubClasses
    get(): Set<KClass<out T>> = NoCachedKlassIndex.getSubclasses(this)
