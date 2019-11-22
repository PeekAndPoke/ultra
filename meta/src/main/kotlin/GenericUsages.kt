package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import de.peekandpoke.ultra.common.startsWithNone
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.MVariable
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

class GenericUsages(override val ctx: ProcessorUtils.Context) : ProcessorUtils {

    private val registry = mutableMapOf<ClassName, MutableSet<MType>>()

    fun get(cls: ClassName): Set<MType> = registry[cls] ?: setOf()

    fun add(type: TypeElement) = apply {

        //        variables
//            .map { it.asTypeName() }
//            .filterIsInstance<ParameterizedTypeName>()
//            .forEach { type ->
//                registry.getOrPut(type.rawType) { mutableSetOf() }.add(type)
//            }

        type.variables.blacklist()
            .asSequence()
            .filter { it.asTypeName() is ParameterizedTypeName }
            .map {

                val parameterized = it.asTypeName() as ParameterizedTypeName


                val variableType = (it.asType() as DeclaredType).asElement() as TypeElement

                fun typeIdx(typeVar: TypeVariableName) =
                    variableType.typeParameters.indexOfFirst { it.simpleName.toString() == typeVar.name }

                fun reify(type: TypeName): TypeName = when (type) {

                    is TypeVariableName -> parameterized.typeArguments[typeIdx(type)]

                    is ParameterizedTypeName -> with(ParameterizedTypeName) {

                        type.rawType.parameterizedBy(
                            type.typeArguments.map { arg -> reify(arg) }
                        )
                    }

                    else -> type
                }

                val reifiedVars = variableType.variables.map {
                    MVariable(ctx, it, reify(it.asTypeName()))
                }


                MType(
                    ctx,
                    variableType,
                    it.asTypeName(),
                    { emptySet() },
                    reifiedVars
                )
            }
            .forEach { type ->
                registry.getOrPut(type.className) { mutableSetOf() }.add(type)
            }
    }

    private fun map(type: TypeElement, parameterized: ParameterizedTypeName) {

    }

    private val blacklisted = arrayOf("java.", "javax.", "javafx.", "kotlin.")

    // Black list some packages
    private fun <T : Element> List<T>.blacklist() = asSequence()
        .distinct()
        .filter { it.fqn.startsWithNone(blacklisted) }
        .toList()
}
