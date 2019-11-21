package de.peekandpoke.ultra.meta.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import de.peekandpoke.ultra.common.ucFirst
import de.peekandpoke.ultra.common.unshift
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

    val simpleName = className.simpleName

    val packageName = className.packageName

    val genericUsages by lazy { genericUsagesProvider() }

    val nesting: List<ClassName> by lazy {

        val all = mutableListOf(className)
        var cls = className

        while (cls.enclosingClassName() != null) {
            all.unshift(cls.enclosingClassName()!!)
            cls = cls.enclosingClassName()!!
        }

        all
    }

    val nestedName = nesting.joinToString(".") { it.simpleName }

    val nestedFileName = nesting.joinToString("_") { it.simpleName }

    val typeArguments = when (typeName) {
        is ParameterizedTypeName -> typeName.typeArguments

        else -> emptyList()
    }

    val methods = type.enclosedElements.filterIsInstance<ExecutableElement>().map {
        MMethod(
            ctx,
            it
        )
    }

    fun hasPublicGetterFor(v: MVariable) = methods.any {
        it.simpleName == "get${v.simpleName.ucFirst()}" && it.isPublic
    }
}
