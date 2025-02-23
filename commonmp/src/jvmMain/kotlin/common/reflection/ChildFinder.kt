package de.peekandpoke.ultra.common.reflection

import de.peekandpoke.ultra.common.prepend
import de.peekandpoke.ultra.common.reflection.ChildFinder.Companion.find
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
    private val visited = mutableSetOf<Int>()

    /** A set of nodes that match the filter criteria */
    private val found = mutableSetOf<Found<C>>()

    data class Found<C>(val item: C, val path: String, val parents: List<Any>) {
        /**
         * Get's the parent at the given [idx] or null
         */
        fun parent(idx: Int): Any? {
            return parents.getOrNull(idx)
        }
    }

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

        visit(inTarget, "root", emptyList())

        return found.toList()
    }

    private fun visit(target: Any?, path: String, parents: List<Any>) {

        // TODO: We need special tests for this, because data class collide with their own hashcode.
        //       Still if we have multiple data classes with same hashCode the identityHashCode should be different.
        val idHashCode = System.identityHashCode(target)

        if (target == null || visited.contains(idHashCode)) {
            return
        }

        visited.add(idHashCode)

        @Suppress("UNCHECKED_CAST")
        if (target::class == searchedClass && predicate(target as C)) {
            found.add(
                Found(
                    item = target,
                    path = path,
                    parents = parents,
                )
            )
            return
        }

        when (target) {
            is List<*> -> visitCollection(target, path, parents)
            is Map<*, *> -> visitCollection(target.values, path, parents)
            else -> visitObject(target, path, parents)
        }
    }

    private fun visitCollection(collection: Collection<*>, path: String, parents: List<Any>) {

        val parentsWithCollection = parents.prepend(collection)

        collection.forEachIndexed { idx, it -> visit(it, "$path.$idx", parentsWithCollection) }
    }

    private fun visitObject(target: Any, path: String, parents: List<Any>) {

        val parentsWithTarget = parents.prepend(target)

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
                    path = "$path.${it.name}",
                    parentsWithTarget
                )
            }
    }
}
