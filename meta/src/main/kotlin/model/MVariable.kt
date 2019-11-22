package de.peekandpoke.ultra.meta.model

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType

class MVariable(
    model: Model,
    val element: VariableElement,
    val typeName: TypeName
) : MBase(model) {

    val type: TypeElement? = when (val elemType = element.asType()) {

        is DeclaredType -> elemType.asElement() as? TypeElement

        else -> null
    }

    val javaFqn = typeName.fqn

    val kotlinFqn = javaFqn.asKotlinClassName()

    val isDelegate = element.simpleName.contains("${"$"}delegate")

    val isNullable = element.isNullable

    val simpleName: String = element.simpleName.toString()

    val isGeneric: Boolean = typeName is TypeVariableName
}
