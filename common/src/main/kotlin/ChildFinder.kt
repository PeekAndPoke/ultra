package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.ChildFinder.Companion.find
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Helper class that descends into an object to find all children and grand-children of the given type
 *
 * Use the [find] methods to initiate the search.
 *
 * TODO: tests
 */
class ChildFinder<C : Any, T : Any> private constructor(
    private val searchedClass: KClass<C>,
    private val inTarget: T,
    private val predicate: (C) -> Boolean
) {
    /** A set of nodes that we already visited. Used to break cyclic graphs */
    private val visited = mutableSetOf<Any>()
    /** A set of nodes that match the filter criteria */
    private val found = mutableSetOf<Found<C>>()

    data class Found<C>(val path: String, val item: C)

    companion object {
        /**
         * Finds all children of [target] that are of exactly of the class [cls]
         */
        fun <C : Any, T : Any> find(cls: KClass<C>, target: T) = find(cls, target) { true }

        /**
         * Finds all children of [target] that are of exactly of the class [cls] and that match the [predicate]
         */
        fun <C : Any, T : Any> find(cls: KClass<C>, target: T, predicate: (C) -> Boolean) =
            ChildFinder(cls, target, predicate).run()
    }

    fun run(): List<Found<C>> {
        visited.clear()
        found.clear()

        visit(inTarget, "root")

        return found.toList()
    }

    private fun visit(target: Any?, path: String) {

        if (target == null || visited.contains(target)) {
            return
        }

        visited.add(target)

        @Suppress("UNCHECKED_CAST")
        if (target::class == searchedClass && predicate(target as C)) {
            found.add(Found(path, target))
            return
        }

        when (target) {
            is List<*> -> visitCollection(target, path)
            is Map<*, *> -> visitCollection(target.values, path)
            else -> visitObject(target, path)
        }
    }

    private fun visitCollection(collection: Collection<*>, path: String) =
        collection.forEachIndexed { idx, it -> visit(it, "$path.$idx") }

    private fun visitObject(target: Any, path: String) {

        val reflect = target::class

        // Blacklist internal java and kotlin classes
        val fqn = reflect.qualifiedName
        if (fqn != null && (fqn.startsWith("java.") || fqn.startsWith("kotlin."))) {
            return
        }

        reflect.memberProperties
            .forEach {
                visit(
                    target = it.javaField?.apply { it.isAccessible = true }?.get(target),
                    path = "$path.${it.name}"
                )
            }
    }
}

