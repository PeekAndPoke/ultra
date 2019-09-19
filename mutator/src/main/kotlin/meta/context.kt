package de.peekandpoke.ultra.mutator.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import javax.lang.model.element.TypeElement

typealias GenericUsages = Map<ClassName, Set<ParameterizedTypeName>>

data class Context(
    val types: List<TypeElement>,
    val genericsUsages: GenericUsages
) {
    fun getGenericUsagesForType(type: ClassName): Set<ParameterizedTypeName> = genericsUsages[type] ?: setOf()

    fun getGenericUsagesForType(type: TypeName): Set<ParameterizedTypeName> = when (type) {
        is ParameterizedTypeName -> getGenericUsagesForType(type.rawType)

        else -> setOf()
    }
}
