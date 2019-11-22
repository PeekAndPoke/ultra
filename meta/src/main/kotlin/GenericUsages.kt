package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import de.peekandpoke.ultra.common.startsWithNone
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.Model
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

class GenericUsages(val model: Model) : ProcessorUtils {

    override val ctx = model.ctx

    private val registry = mutableMapOf<ClassName, MutableSet<MType>>()

    fun get(cls: ClassName): Set<MType> = registry[cls] ?: setOf()

    fun add(type: TypeElement) = apply {

        type.variables.blacklist()
            .asSequence()
            .filter { it.asTypeName() is ParameterizedTypeName }
            .map {

                val parameterized = it.asTypeName() as ParameterizedTypeName

                val variableType = (it.asType() as DeclaredType).asElement() as TypeElement

                MType(model, variableType, parameterized)

            }
            .forEach { type ->
                registry.getOrPut(type.className) { mutableSetOf() }.add(type)
            }
    }

    private val blacklisted = arrayOf("java.", "javax.", "javafx.", "kotlin.")

    // Black list some packages
    private fun <T : Element> List<T>.blacklist() = asSequence()
        .distinct()
        .filter { it.fqn.startsWithNone(blacklisted) }
        .toList()
}
