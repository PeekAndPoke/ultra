package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MVariable

interface PropertyRenderer : ProcessorUtils {

    /**
     * Returns 'true' when the renderer can handle the given VariableElement
     */
    fun canHandle(type: TypeName): Boolean

    /**
     * Renders code-blocks for a property
     */
    fun KotlinPrinter.renderProperty(variable: MVariable)

    fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int)

    fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int)

    val Int.asParam get() = "it$this"

    val Int.asOnModify get() = "on$this"

    fun KotlinPrinter.renderVariableComment(variable: MVariable) {

        val imported = variable.parent.import()
        val type = variable.typeName
        val prop = variable.simpleName

        block(
            """
                /**
                 * Mutator for field [${imported}.$prop]
                 *
                 * Info:
                 *   - type:         [${variable.typeName.import()}]
                 *   - reflected by: [${type::class.qualifiedName}]
                 */ 
            """.trimIndent()
        )

    }
}
