package de.peekandpoke.ktorfx.rest.security

import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.common.reflection.TypeRef
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf


class ReflectivePathFinder private constructor(
    private val root: KType,
    private val predicate: (ItemToCheck) -> Boolean,
    private val hardExcludes: List<(KClass<*>) -> Boolean>,
) {
    companion object {
        val defaultHardExcludes = listOf<(KClass<*>) -> Boolean>(
            { it.qualifiedName == null },
            { it.qualifiedName!!.startsWith("kotlin.") },
            { it.qualifiedName!!.startsWith("java.") },
            { it.qualifiedName!!.startsWith("javax.") },
            { it.qualifiedName!!.startsWith("google.") },
        )

        fun <T> TypeRef<T>.findAnnotatedElementPaths(predicate: (ItemToCheck) -> Boolean): List<FoundItem> {
            return type.findAnnotatedElementPaths(predicate)
        }

        fun KType.findAnnotatedElementPaths(predicate: (ItemToCheck) -> Boolean): List<FoundItem> {
            return ReflectivePathFinder(
                root = this,
                predicate = predicate,
                hardExcludes = defaultHardExcludes,
            ).run()
        }
    }

    sealed class ItemToCheck {
        inline fun <reified T : Annotation> hasAnnotation() = when (this) {
            is Type -> itemHasAnnotation<T>()
            is Param -> itemHasAnnotation<T>()
        }

        class Type(val type: KType) : ItemToCheck() {
            inline fun <reified T : Annotation> itemHasAnnotation(): Boolean {
                return (type.classifier as? KClass<*>)
                    ?.hasAnnotation<T>()
                    ?: false
            }
        }

        class Param(val param: KParameter) : ItemToCheck() {
            inline fun <reified T : Annotation> itemHasAnnotation(): Boolean {
                return param.hasAnnotation<T>()
            }
        }
    }

    data class FoundItem(
        val path: List<String>,
        val type: KType,
    )

    val visited = mutableSetOf<KType>()
    val found = mutableListOf<FoundItem>()

    fun run(): List<FoundItem> {

//        println("running for type: $root")

        visit(root, emptyList())

        return found.sortedBy { it.path.joinToString(".") }
    }

    fun visit(visitedType: KType, path: List<String>) {

        // Did we already visit this type? The we stop to avoid endless loops.
        if (!visited.add(visitedType)) {
            return
        }

//        println(path.joinToString("."))

        val cls = visitedType.classifier as? KClass<*> ?: return

        // Dive in to collection types
        val isCollectionType = cls.isSubclassOf(Collection::class)
                || cls.isSubclassOf(Map::class)
                || cls.isSubclassOf(Array::class)

        if (isCollectionType) {
            visitedType.arguments.forEach { projection ->
                projection.type?.let { visit(it, path.plus("*")) }
            }

            return
        }

        if (!visitedType.isAcceptable()) {
            return
        }

        // Check this type
        if (predicate(ItemToCheck.Type(visitedType))) {
            found.add(
                FoundItem(path = path, type = visitedType)
            )
        }

        if (visitedType.isDataClass()) {

            // Check all constructor parameters
            ReifiedKType(visitedType).ctorParams2Types.forEach { (param, reifiedParamType) ->
                val nextPath = path.plus(param.name ?: "n/a")

                if (predicate(ItemToCheck.Param(param))) {
                    found.add(
                        FoundItem(path = nextPath, type = reifiedParamType)
                    )
                }

                visit(reifiedParamType, nextPath)
            }

            return
        }

        // TODO: what if we reach here?
    }

    private fun KType.isDataClass(): Boolean {
        return (classifier as? KClass<*>)?.isData ?: false
    }

    private fun KType.isAcceptable(): Boolean {
        val cls = classifier as? KClass<*> ?: return false
        val java = cls.java

        if (hardExcludes.any { it(cls) }) {
            return false
        }

        if (java.isEnum) {
            return true
        }

        return !java.isSynthetic &&
                // filter out primitive type
                !java.isPrimitive &&
                // no builtin type
                cls !in listOf(Any::class, Enum::class, Number::class, String::class, Nothing::class, Unit::class)
    }
}
