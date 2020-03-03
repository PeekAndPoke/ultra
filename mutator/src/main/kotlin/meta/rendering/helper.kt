package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.common.md5
import de.peekandpoke.ultra.meta.model.MType

val MType.mutatorClassName: ClassName get() = typeName.mutatorClassName

val TypeName.mutatorClassName: ClassName
    get() = when (val tn = this) {

        is ParameterizedTypeName -> {
            val hash = tn.hashTypeArguments()
            val raw = tn.rawType

            ClassName(
                packageName = raw.packageName,
                simpleNames = raw.simpleNames.dropLast(1).plus(raw.simpleNames.last() + "Mutator_$hash")
            )
        }

        is ClassName -> ClassName(
            packageName = tn.packageName,
            simpleNames = tn.simpleNames.dropLast(1).plus(tn.simpleNames.last() + "Mutator")
        )

        else -> error("Cannot create mutator class name for '${tn}'")
    }

fun ParameterizedTypeName.hashTypeArguments() = typeArguments.joinToString { it.toString() }.md5()
