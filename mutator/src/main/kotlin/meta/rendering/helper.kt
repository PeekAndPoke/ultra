package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.common.md5
import de.peekandpoke.ultra.meta.model.MType

val MType.mutatorClassName
    get() = when {

        isParameterized -> {
            val hash = (typeName as ParameterizedTypeName).hashTypeArguments()

            joinSimpleNames("_") + "Mutator_$hash"
        }

        else -> joinSimpleNames("_") + "Mutator"
    }

val TypeName.mutatorFqn
    get() = when (this) {
        is ClassName -> "$packageName.$mutatorClassName"
        is ParameterizedTypeName -> rawType.packageName + "." + rawType.mutatorClassName + "_" + hashTypeArguments()
        else -> toString() + "Mutator"
    }

val ClassName.mutatorClassName get() = simpleNames.joinToString("_") + "Mutator"

fun ParameterizedTypeName.hashTypeArguments() = typeArguments.joinToString { it.toString() }.md5()
