package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MVariable

/**
 * Renderer for primitive types and Strings
 */
class PureGetterSetterRenderer(
    override val ctx: ProcessorUtils.Context
) : PropertyRenderer {

    override fun canHandle(type: TypeName) = true

    override fun KotlinPrinter.renderPropertyDeclaration(variable: MVariable) {

        val name = variable.simpleName

        renderVariableComment(variable)

        block(
            """
                var $name
            """.trimIndent()
        )

        newline()
    }

    override fun KotlinPrinter.renderPropertyImplementation(variable: MVariable) {

        val name = variable.simpleName

        renderVariableComment(variable)

        block(
            """
                override var $name
                    get() = getResult().$name
                    set(v) = modify(getResult()::$name, getResult().$name, v)
            """.trimIndent()
        )

        newline()
    }

    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {
        append(depth.asParam)
    }

    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {
        append(depth.asParam)
    }
}
