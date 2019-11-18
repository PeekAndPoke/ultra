package de.peekandpoke.ultra.mutator.meta

import com.squareup.kotlinpoet.*
import de.peekandpoke.ultra.common.startsWithNone
import de.peekandpoke.ultra.common.unshift
import de.peekandpoke.ultra.meta.ProcessorUtils
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class GenericUsages(

    override val logPrefix: String,
    override val processingEnv: ProcessingEnvironment

) : ProcessorUtils {

    val registry = mutableMapOf<ClassName, MutableSet<ParameterizedTypeName>>()

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

data class Context(val types: List<TypeElement>, val genericsUsages: GenericUsages)

val TypeElement.info get() = FullTypeInfo(this.asClassName(), this.asType().asTypeName())

val ParameterizedTypeName.info get() = FullTypeInfo(this.rawType, this)

data class FullTypeInfo(val className: ClassName, val typeName: TypeName) {

    val fqn = typeName.toString()
    val packageName get() = className.packageName
    val simpleName get() = className.simpleName

    val typeParamsStr by lazy {
        when (typeName) {
            is ParameterizedTypeName ->
                "<" + typeName.typeArguments.mapIndexed { idx, _ -> "P${idx}" }.joinToString(", ") + ">"

            else -> ""
        }
    }

    val receiverStr by lazy {

        // We have to take care of any enclosing classes
        val all = mutableListOf(simpleName)
        var cls = className

        while (cls.enclosingClassName() != null) {
            all.unshift(cls.enclosingClassName()!!.simpleName)
            cls = cls.enclosingClassName()!!
        }

        "${all.joinToString(".")}$typeParamsStr"
    }

    val mutatorClassStr by lazy {
        "${simpleName}Mutator$typeParamsStr"
    }

    fun mutatorClass(typeParams: ParameterizedTypeName): String {
        return "${simpleName}Mutator<${typeParams.typeArguments.joinToString(", ")}>"
    }
}
