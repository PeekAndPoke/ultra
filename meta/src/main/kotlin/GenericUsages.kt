package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import de.peekandpoke.ultra.common.startsWithNone
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class GenericUsages(override val ctx: ProcessorUtils.Context) : ProcessorUtils {

    private val registry = mutableMapOf<ClassName, MutableSet<ParameterizedTypeName>>()

    fun get(cls: ClassName): Set<ParameterizedTypeName> {
        return registry[cls] ?: setOf()
    }

    fun add(type: TypeElement) = apply {
        add(type.variables.blacklist().filter { it.asTypeName() is ParameterizedTypeName })
    }

    private fun add(variables: List<VariableElement>) = apply {

        variables
            .map { it.asTypeName() }.filterIsInstance<ParameterizedTypeName>()
            .forEach { type ->
                registry.getOrPut(type.rawType) { mutableSetOf() }.add(type)
            }
    }

    private val blacklisted = arrayOf("java.", "javax.", "javafx.", "kotlin.")

    // Black list some packages
    private fun <T : Element> List<T>.blacklist() = asSequence()
        .distinct()
        .filter { it.fqn.startsWithNone(blacklisted) }
        .toList()
}
