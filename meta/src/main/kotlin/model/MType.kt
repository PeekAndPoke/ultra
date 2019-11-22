package de.peekandpoke.ultra.meta.model

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import de.peekandpoke.ultra.common.ucFirst
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class MType(
    model: Model,
    val type: TypeElement,
    val typeName: TypeName
) : MBase(model) {

    val isParameterized = typeName is ParameterizedTypeName

    val className = type.asClassName()

    val packageName = className.packageName

    val genericUsages by lazy { model.getGenericUsages(className) }

    val variables: List<MVariable> by lazy {
        when (typeName) {
            is ParameterizedTypeName ->
                type.variables.map { MVariable(model, it, typeName.reify(it.asTypeName())) }

            else ->
                type.variables.map { MVariable(model, it, it.asTypeName()) }
        }
    }

    val methods: List<MMethod> by lazy {
        type.enclosedElements
            .filterIsInstance<ExecutableElement>()
            .map { MMethod(model, it) }
    }

    fun joinSimpleNames(glue: String = "_") = className.simpleNames.joinToString(glue)

    fun hasPublicGetterFor(v: MVariable) = methods.any {
        it.simpleName == "get${v.simpleName.ucFirst()}" && it.isPublic
    }

    /**
     * Get the index of a type variable
     */
    private fun TypeElement.typeParamIdx(typeVar: TypeVariableName) = typeParameters.indexOfFirst { typeParam ->
        typeParam.simpleName.toString() == typeVar.name
    }

    /**
     * Reifies the [generic] TypeName by looking up the indexes of type parameters in [type]
     */
    private fun ParameterizedTypeName.reify(generic: TypeName): TypeName = when (generic) {

        is TypeVariableName -> typeArguments[type.typeParamIdx(generic)]

        is ParameterizedTypeName -> with(ParameterizedTypeName) {

            generic.rawType.parameterizedBy(
                generic.typeArguments.map { arg -> reify(arg) }
            )
        }

        else -> generic
    }
}
