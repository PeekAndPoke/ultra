package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ClassName
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
                // we look for a ClassName
                type is ClassName &&
                // we exclude enum classes
                !type.isEnum &&
                // we also exclude some packages completely
                !type.isBlackListed

    override fun KotlinPrinter.renderPropertyDeclaration(variable: MVariable) {

        val type = variable.typeName as ClassName
        val name = variable.simpleName

        val nullable = if (type.isNullable) "?" else ""

        val mutatorImported = type.toMutatorClassName().import()

        renderVariableComment(variable)

        block(
            """
                var $name : $mutatorImported$nullable
    
            """.trimIndent()
        )
    }

    override fun KotlinPrinter.renderPropertyImplementation(variable: MVariable) {

        val type = variable.typeName as ClassName
        val name = variable.simpleName

        val nullable = if (type.isNullable) "?" else ""

        val mutatorField = "`$name@mutator`"
        val mutatorImported = type.toMutatorClassName().import()

        block(
            """
                /**
                 * Backing field for [$name]
                 */
                private var $mutatorField : $mutatorImported? = null
                 
            """.trimIndent()
        )

        renderVariableComment(variable)

        block(
            """
                override var $name : $mutatorImported$nullable
                    get() = $mutatorField ?: getResult().$name$nullable.${type.import("mutator")} { 
                        modify(getResult()::$name, getResult().$name, it) 
                    }.apply {
                        $mutatorField = this
                    }
                    set(value) {
                        $mutatorField = null
                        modify(getResult()::$name, getResult().$name, value$nullable.getResult()) 
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
