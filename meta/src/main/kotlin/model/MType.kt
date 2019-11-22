package de.peekandpoke.ultra.meta.model

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import de.peekandpoke.ultra.common.ucFirst
import de.peekandpoke.ultra.meta.ProcessorUtils
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

data class MType(
    override val ctx: ProcessorUtils.Context,
    val type: TypeElement,
    val typeName: TypeName,
    private val genericUsagesProvider: () -> Set<MType>,
    val variables: List<MVariable>
) : ProcessorUtils {

    val isParameterized = typeName is ParameterizedTypeName

    val className = type.asClassName()

    val packageName = className.packageName

    val genericUsages by lazy { genericUsagesProvider() }

    val methods = type.enclosedElements.filterIsInstance<ExecutableElement>().map {
        MMethod(
            ctx,
            it
        )
    }

    fun joinSimpleNames(glue: String = "_") = className.simpleNames.joinToString(glue)

    fun hasPublicGetterFor(v: MVariable) = methods.any {
        it.simpleName == "get${v.simpleName.ucFirst()}" && it.isPublic
    }
}
