package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MVariable

/**
 * Here we handle non parameterized data classes
 */
class DataClassPropertyRenderer(
    override val ctx: ProcessorUtils.Context
) : PropertyRenderer {

    // TODO: check if the type has a "copy" method
    override fun canHandle(type: TypeName) =
        // exclude blank name (probably a generic type like T)
        type.packageName.isNotEmpty() &&
                // we exclude enum classes
                !type.isEnum &&
                // we also exclude some packages completely
                !type.isBlackListed

    override fun KotlinPrinter.renderProperty(variable: MVariable) {

        val type = variable.typeName
        val name = variable.simpleName

        val nullable = if (type.isNullable) "?" else ""

        val mutatorField = "`$name@mutator`"

        val typeImported = type.mutatorFqn

        block(
            """
                /**
                 * Backing field for [$name]
                 */
                private var $mutatorField : $typeImported? = null
                 
            """.trimIndent()
        )

        renderVariableComment(variable)

        block(
            """
                val $name
                    get() = $mutatorField ?: getResult().$name$nullable.${type.import("mutator")} { 
                        modify(getResult()::$name, getResult().$name, it) 
                    }.apply {
                        $mutatorField = this
                    }
    
            """.trimIndent()
        )
    }

    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {

        append("${depth.asParam}.${type.import("mutator")}(${depth.asOnModify})")
    }

    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {

        append("${depth.asParam}.getResult()")
    }
}
