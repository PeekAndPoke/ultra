package de.peekandpoke.ultra.slumber

import org.atteo.classindex.ClassIndex
import org.atteo.classindex.IndexSubclasses
import kotlin.reflect.KClass

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
 *
 * The result of this method call will be cached. When you are looking for an uncached version please use
 * [indexedSubClassesUncached]
 */
internal val <T : Any> KClass<T>.indexedSubClasses get(): Set<KClass<out T>> = SubClassIndexCache.get(this)

/**
 * A set of sub-classes indexed by [ClassIndex]
 *
 * To make this work, the parent class needs to be annotated with [IndexSubclasses]
 */
internal val <T : Any> KClass<T>.indexedSubClassesUncached
    get(): Set<KClass<out T>> = SubClassIndexCache.getUncached(
        this
    )

/**
 * Internal helper class for caching the result of looking up indexed sub classes
 */
internal object SubClassIndexCache {

    private val cache = mutableMapOf<KClass<*>, Set<KClass<*>>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(cls: KClass<T>): Set<KClass<out T>> = cache.getOrPut(cls) {
        getUncached(cls)
    } as Set<KClass<out T>>

    fun <T : Any> getUncached(cls: KClass<T>): Set<KClass<out T>> {
        val result = mutableSetOf<KClass<out T>>()

        var t: ((loader: ClassLoader?) -> Unit)? = null

        val tryLoad: (ClassLoader?) -> Unit = { loader: ClassLoader? ->
            if (loader != null) {
                result.addAll(
                    ClassIndex.getSubclasses(cls.java, loader).map { it.kotlin }
                )

                t!!(loader.parent)
            }
        }

        t = tryLoad

        // With classes class loader
        tryLoad(cls.java.classLoader)

        // With context class loader
        tryLoad(Thread.currentThread().contextClassLoader)

        return result
    }
}
