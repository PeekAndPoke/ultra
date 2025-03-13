package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MVariable

class CollectionSetPropertyRenderer(
    ctx: ProcessorUtils.Context,
    root: PropertyRenderers
) : CollectionPropertyRendererBase(ctx, root) {

    /**
     * Types supported by this renderer
     */
    private val supported = listOf(
        "java.util.Set"
    )

    /**
     * Returns 'true' when the handler supports the given [type]
     *
     * @see PropertyRenderer.canHandle for details
     */
    override fun canHandle(type: TypeName) = type is ParameterizedTypeName &&
            // is the type supported?
            supported.contains(type.rawType.fqn) &&
            // and the contained type must be supported as well
            type.typeArguments.all { root.canHandle(it) }

    /**
     * Renders the code for a property implementation in a class
     *
     * @see PropertyRenderer.renderPropertyImplementation for details
     */
    override fun KotlinPrinter.renderPropertyImplementation(variable: MVariable) {
        // get the type name of the variable
        val type = variable.typeName
        // get the simple name of the variable
        val name = variable.simpleName

        renderVariableComment(variable)

        internalRenderProperty(name, (type as ParameterizedTypeName).typeArguments[0])
    }

    /**
     * Renders the code that maps a value to its mutator.
     *
     * @see PropertyRenderer.renderForwardMapper for details
     */
    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {
        internalRenderForwardMapper((type as ParameterizedTypeName).typeArguments[0], depth)
    }

    /**
     * Renders the code that maps a mutator back to its value.
     *
     * @see PropertyRenderer.renderBackwardMapper for details
     */
    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {
        internalRenderBackwardMapper(depth)
    }
}
