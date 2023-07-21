package de.peekandpoke.ultra

import com.github.matfax.klassindex.SubclassIndex
import kotlin.reflect.KClass

@Suppress("unused")
const val COMMON_MP_JVM = "Hello CommonMpJvm!"

object NoCachedKlassIndex {

    private val cache = mutableMapOf<ClassLoader, MutableMap<KClass<*>, Set<KClass<*>>>>()

    fun <T : Any> getSubclasses(cls: KClass<T>): Set<KClass<T>> {
        val loader = cls.java.classLoader

        @Suppress("UNCHECKED_CAST")
        return cache
            .getOrPut(loader) { mutableMapOf() }
            .getOrPut(cls) { loader.internalGetSubClasses(cls) } as Set<KClass<T>>
    }

    fun <T : Any> getSubclassesUncached(cls: KClass<T>): Set<KClass<T>> {
        val loader = cls.java.classLoader

        return loader.internalGetSubClasses(cls)
    }

    private fun <T : Any> ClassLoader.internalGetSubClasses(cls: KClass<T>): Set<KClass<T>> {

        val result = SubclassIndex.index()
            .filter { (k, _) -> k.qualifiedName == cls.qualifiedName }
            .map { (_, v) -> v }
            .firstOrNull()
            .orEmpty()
            .filterIsInstance<KClass<T>>()
            .map { subClass: KClass<T> ->
                when (val clsName = subClass.java.name) {
                    null -> subClass
                    else -> loadClass(clsName).kotlin
                }
            }
            .toSet()

        @Suppress("UNCHECKED_CAST")
        return result as Set<KClass<T>>
    }
}
