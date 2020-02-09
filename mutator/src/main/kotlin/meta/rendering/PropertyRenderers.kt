package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MVariable

/**
 * A collection of CodeRenderers
 *
 * canHandle() returns 'true' when one of the children returns true for the given VariableElement.
 * .
 * render() returns the code by calling render on the first child that can handle the given VariableElement.
 */
class PropertyRenderers(

    override val ctx: ProcessorUtils.Context,
    private val fallback: PropertyRenderer,
    private val provider: (PropertyRenderers) -> List<PropertyRenderer>

) : PropertyRenderer {

    /**
     * All property renderers
     */
    private val children by lazy { provider(this) }

    /**
     * Internal lookup from [TypeName] to [PropertyRenderer]
     */
    private val cache = mutableMapOf<TypeName, PropertyRenderer>()

    /**
     * We can always handle any type, since we have a fallback
     */
    override fun canHandle(type: TypeName): Boolean = true

    /**
     * Returns the code for the first matching child renderer
     */
    override fun KotlinPrinter.renderProperty(variable: MVariable) =
        match(variable.typeName).run { renderProperty(variable) }

    /**
     * TODO: comment
     */
    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) =
        match(type).run { renderForwardMapper(type, depth) }

    /**
     * TODO: comment
     */
    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) =
        match(type).run { renderBackwardMapper(type, depth) }

    /**
     * Get the [PropertyRenderer] for the given [TypeName]
     */
    private fun match(type: TypeName) = cache.getOrPut(type) {
        children.firstOrNull { it.canHandle(type) } ?: fallback
    }
}
