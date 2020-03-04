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
     * Renders the code that implements a property in a class
     */
    fun KotlinPrinter.renderPropertyImplementation(variable: MVariable)

    /**
     * Renders the code that maps a value to its mutator.
     *
     * If there is no mutator for the value, e.g. for primitive type, it should render the identity function.
     */
    fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int)

    /**
     * Renders the code that maps a mutator back to its value
     *
     * If there is no mutator for the value, e.g. for primitive type, it should render the identity function.
     */
    fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int)

    /**
     * Helper function that converts an Int to a parameter name, e.g. it1, it2, ...
     */
    val Int.asParam get() = "it$this"

    /**
     * Helper function that converts an Int to a "onModify" function name, e.g. on1, on2, ...
     */
    val Int.asOnModify get() = "on$this"

    /**
     * Helper that renders the comment block for a field inside of a mutator
     */
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
