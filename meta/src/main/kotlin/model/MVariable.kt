package de.peekandpoke.ultra.meta.model

import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import de.peekandpoke.ultra.meta.ProcessorUtils
import javax.lang.model.element.VariableElement

data class MVariable(
    override val ctx: ProcessorUtils.Context,
    val element: VariableElement
) : ProcessorUtils {
    val fqn = element.fqn

    val typeName = element.asType().asTypeName()

    val kotlinClass = fqn.asKotlinClassName()

    val isDelegate = element.simpleName.contains("${"$"}delegate")

    val isNullable = element.isNullable

    val simpleName: String = element.simpleName.toString()

    val isGeneric: Boolean = typeName is TypeVariableName
}
